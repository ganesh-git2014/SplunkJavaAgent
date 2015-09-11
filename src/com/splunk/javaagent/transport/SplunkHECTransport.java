package com.splunk.javaagent.transport;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.log4j.Logger;

import com.splunk.javaagent.SplunkLogEvent;
import com.splunk.javaagent.jmx.mbean.HECTransportMXBean;

public class SplunkHECTransport extends SplunkInput implements SplunkTransport,
		HECTransportMXBean {

	private static Logger logger = Logger.getLogger(SplunkHECTransport.class);

	private String host = "localhost";
	private int port = 8088;
	private boolean https = false;
	private int poolsize = 1;
	private String token;
	private String index = "main";
	private String source = "javaagent_input_hec";
	private String sourcetype = "javaagent";

	private boolean batchMode = false;
	private long maxBatchSizeBytes = 1 * MB; // 1MB
	private long maxBatchSizeEvents = 100; // 100 events
	private long maxInactiveTimeBeforeBatchFlush = 5000;// 5 secs

	// batch buffer
	private List<String> batchBuffer;
	private long currentBatchSizeBytes = 0;
	private long lastEventReceivedTime;

	private CloseableHttpAsyncClient httpClient;
	private URI uri;

	private static final HostnameVerifier HOSTNAME_VERIFIER = new HostnameVerifier() {
		public boolean verify(String s, SSLSession sslSession) {
			return true;
		}
	};

	@Override
	public void init(Map<String, String> args) throws Exception {

		logger.info("Initialising HEC transport");

		// required
		setToken(args.get("splunk.transport.hec.token"));

		try {
			setHost(args.get("splunk.transport.hec.host"));
		} catch (Exception e) {

		}
		try {
			setPort(Integer.parseInt(args.get("splunk.transport.hec.port")));
		} catch (Exception e) {

		}
		try {
			setPoolSize(Integer.parseInt(args
					.get("splunk.transport.hec.poolsize")));
		} catch (Exception e) {

		}

		try {
			setHTTPs(Boolean.parseBoolean(args
					.get("splunk.transport.hec.https")));
		} catch (Exception e) {

		}

		try {
			setBatchMode(Boolean.parseBoolean(args
					.get("splunk.transport.hec.batchMode")));
		} catch (Exception e) {

		}
		try {
			setMaxBatchSizeBytes(args
					.get("splunk.transport.hec.maxBatchSizeBytes"));
		} catch (Exception e) {

		}
		try {
			setMaxBatchSizeEvents(Long.parseLong(args
					.get("splunk.transport.hec.maxBatchSizeEvents")));
		} catch (Exception e) {

		}
		try {
			setMaxInactiveTimeBeforeBatchFlush(Long
					.parseLong(args
							.get("splunk.transport.hec.maxInactiveTimeBeforeBatchFlush")));
		} catch (Exception e) {

		}

		try {
			setSource(args.get("splunk.transport.hec.source"));
		} catch (Exception e) {

		}
		try {
			setSourcetype(args.get("splunk.transport.hec.sourcetype"));
		} catch (Exception e) {

		}
		try {
			setIndex(args.get("splunk.transport.hec.index"));
		} catch (Exception e) {

		}

		try {
			setDropEventsOnQueueFull(Boolean.parseBoolean(args
					.get("splunk.transport.hec.dropEventsOnQueueFull")));
		} catch (Exception e) {

		}
		try {
			setMaxQueueSize(args.get("splunk.transport.hec.maxQueueSize"));
		} catch (Exception e) {

		}
	}

	@Override
	public void start() throws Exception {

		logger.info("Starting HEC transport");

		this.batchBuffer = Collections
				.synchronizedList(new LinkedList<String>());
		this.lastEventReceivedTime = System.currentTimeMillis();

		Registry<SchemeIOSessionStrategy> sslSessionStrategy = RegistryBuilder
				.<SchemeIOSessionStrategy> create()
				.register("http", NoopIOSessionStrategy.INSTANCE)
				.register(
						"https",
						new SSLIOSessionStrategy(getSSLContext(),
								HOSTNAME_VERIFIER)).build();

		ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor();
		PoolingNHttpClientConnectionManager cm = new PoolingNHttpClientConnectionManager(
				ioReactor, sslSessionStrategy);
		cm.setMaxTotal(getPoolSize());

		HttpHost splunk = new HttpHost(getHost(), getPort());
		cm.setMaxPerRoute(new HttpRoute(splunk), getPoolSize());

		httpClient = HttpAsyncClients.custom().setConnectionManager(cm).build();

		uri = new URIBuilder().setScheme(getHTTPs() ? "https" : "http")
				.setHost(getHost()).setPort(getPort())
				.setPath("/services/collector").build();

		httpClient.start();

		if (getBatchMode()) {
			new BatchBufferActivityCheckerThread(this).start();
		}

	}

	class BatchBufferActivityCheckerThread extends Thread {

		SplunkHECTransport parent;

		BatchBufferActivityCheckerThread(SplunkHECTransport parent) {

			this.parent = parent;
		}

		public void run() {

			while (true) {
				String currentMessage = "";
				try {
					long currentTime = System.currentTimeMillis();
					if ((currentTime - parent.lastEventReceivedTime) >= parent
							.getMaxInactiveTimeBeforeBatchFlush()) {
						if (batchBuffer.size() > 0) {
							currentMessage = parent.rollOutBatchBuffer();
							parent.batchBuffer.clear();
							parent.currentBatchSizeBytes = 0;
							parent.hecPost(currentMessage);
						}
					}

					Thread.sleep(1000);
				} catch (Exception e) {
					// something went wrong , put message on the queue for retry
					enqueue(currentMessage);
					try {
						parent.stop();
					} catch (Exception e1) {
					}

					try {
						parent.start();
					} catch (Exception e2) {
					}
				}

			}
		}
	}

	private String rollOutBatchBuffer() {

		StringBuffer sb = new StringBuffer();

		for (String event : batchBuffer) {
			sb.append(event);
		}

		return sb.toString();
	}

	private SSLContext getSSLContext() {
		TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
			public boolean isTrusted(X509Certificate[] certificate,
					String authType) {
				return true;
			}
		};
		SSLContext sslContext = null;
		try {
			sslContext = SSLContexts.custom()
					.loadTrustMaterial(null, acceptingTrustStrategy).build();
		} catch (Exception e) {
			// Handle error
		}
		return sslContext;

	}

	@Override
	public void stop() throws Exception {
		try {

			logger.info("Stopping HEC transport");

			httpClient.close();
		} catch (Exception e) {
		}

	}

	@Override
	public void restart() throws Exception {

		logger.info("Restarting HEC transport");

		stop();
		start();

	}

	@Override
	public void send(SplunkLogEvent event) {

		String currentMessage = "";

		try {
			String message = wrapMessageInQuotes(event.toString());
			// could use a JSON Object , but the JSON is so trivial , just
			// building it with a StringBuffer
			StringBuffer json = new StringBuffer();
			json.append("{\"").append("event\":").append(message).append(",\"")
					.append("index\":\"").append(getIndex()).append("\",\"")
					.append("source\":\"").append(getSource()).append("\",\"")
					.append("sourcetype\":\"").append(getSourcetype())
					.append("\"").append("}");

			currentMessage = json.toString();

			if (getBatchMode()) {

				lastEventReceivedTime = System.currentTimeMillis();
				currentBatchSizeBytes += currentMessage.length();
				batchBuffer.add(currentMessage);

				if (flushBuffer()) {

					currentMessage = rollOutBatchBuffer();
					batchBuffer.clear();
					currentBatchSizeBytes = 0;
					hecPost(currentMessage);
				}
			} else {
				hecPost(currentMessage);
			}

			// flush the queue
			while (queueContainsEvents()) {

				String messageOffQueue = dequeue();
				currentMessage = messageOffQueue;
				hecPost(currentMessage);
			}

		} catch (Exception e) {

			logger.error("Error sending message via HEC transport : "
					+ e.getMessage());

			// something went wrong , put message on the queue for retry
			enqueue(currentMessage);
			try {
				stop();
			} catch (Exception e1) {
			}

			try {
				start();
			} catch (Exception e2) {
			}
		}

	}

	private boolean flushBuffer() {

		return (currentBatchSizeBytes >= getMaxBatchSizeBytes())
				|| (batchBuffer.size() >= getMaxBatchSizeEvents());

	}

	private void hecPost(String currentMessage) throws Exception {
		HttpPost post = new HttpPost(uri);
		post.addHeader("Authorization", "Splunk " + getToken());

		StringEntity requestEntity = new StringEntity(currentMessage,
				ContentType.create("application/json", "UTF-8"));

		post.setEntity(requestEntity);
		Future<HttpResponse> future = httpClient.execute(post, null);
		HttpResponse response = future.get();
		// System.out.println(response.getStatusLine());
		// System.out.println(EntityUtils.toString(response.getEntity()));

	}

	private String wrapMessageInQuotes(String message) {

		return "\"" + message + "\"";
	}

	public String getToken() {
		return this.token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getIndex() {
		return this.index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String getSource() {
		return this.source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getSourcetype() {
		return this.sourcetype;
	}

	public void setSourcetype(String sourcetype) {
		this.sourcetype = sourcetype;
	}

	@Override
	public boolean getDropEventsOnFullQueue() {
		return this.isDropEventsOnQueueFull();
	}

	@Override
	public void setDropEventsOnFullQueue(boolean val) {
		this.setDropEventsOnQueueFull(val);

	}

	@Override
	public boolean getHTTPs() {
		return this.https;
	}

	@Override
	public int getPoolSize() {
		return this.poolsize;
	}

	@Override
	public void setHTTPs(boolean val) {
		this.https = val;

	}

	@Override
	public void setPoolSize(int val) {
		this.poolsize = val;

	}

	public boolean getBatchMode() {
		return batchMode;
	}

	public void setBatchMode(boolean batchMode) {
		this.batchMode = batchMode;
	}

	public long getMaxBatchSizeBytes() {
		return maxBatchSizeBytes;
	}

	public void setMaxBatchSizeBytes(long maxBatchSizeBytes) {
		this.maxBatchSizeBytes = maxBatchSizeBytes;
	}

	/**
	 * Set the bacth size from the configured property String value. If parsing
	 * fails , the default of 500KB will be used.
	 * 
	 * @param rawProperty
	 *            in format [<integer>|<integer>[KB|MB|GB]]
	 */
	public void setMaxBatchSizeBytes(String rawProperty) {

		int multiplier;
		int factor;

		if (rawProperty.endsWith("KB")) {
			multiplier = KB;
		} else if (rawProperty.endsWith("MB")) {
			multiplier = MB;
		} else if (rawProperty.endsWith("GB")) {
			multiplier = GB;
		} else {
			return;
		}
		try {
			factor = Integer.parseInt(rawProperty.substring(0,
					rawProperty.length() - 2));
		} catch (NumberFormatException e) {
			return;
		}
		setMaxBatchSizeBytes(factor * multiplier);

	}

	public long getMaxBatchSizeEvents() {
		return maxBatchSizeEvents;
	}

	public void setMaxBatchSizeEvents(long maxBatchSizeEvents) {
		this.maxBatchSizeEvents = maxBatchSizeEvents;
	}

	public long getMaxInactiveTimeBeforeBatchFlush() {
		return maxInactiveTimeBeforeBatchFlush;
	}

	public void setMaxInactiveTimeBeforeBatchFlush(
			long maxInactiveTimeBeforeBatchFlush) {
		this.maxInactiveTimeBeforeBatchFlush = maxInactiveTimeBeforeBatchFlush;
	}

}

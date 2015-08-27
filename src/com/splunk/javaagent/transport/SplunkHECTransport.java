package com.splunk.javaagent.transport;

import java.net.URI;
import java.util.Map;

import org.apache.http.HttpHost;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.log4j.Logger;

import com.splunk.javaagent.SplunkLogEvent;
import com.splunk.javaagent.jmx.mbean.HECTransportMBean;

public class SplunkHECTransport extends SplunkInput implements SplunkTransport,
		HECTransportMBean {

	private static Logger logger = Logger.getLogger(SplunkHECTransport.class);

	private String host = "localhost";
	private int port = 8088;
	private boolean https = false;
	private int poolsize = 1;
	private String token;
	private String index = "main";
	private String source = "javaagent_input_hec";
	private String sourcetype = "javaagent";

	private CloseableHttpAsyncClient httpClient;
	private URI uri;

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
					.get("splunk.transport.tcp.dropEventsOnQueueFull")));
		} catch (Exception e) {

		}
		try {
			setMaxQueueSize(args.get("splunk.transport.tcp.maxQueueSize"));
		} catch (Exception e) {

		}
	}

	@Override
	public void start() throws Exception {

		logger.info("Starting HEC transport");

		ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor();
		PoolingNHttpClientConnectionManager cm = new PoolingNHttpClientConnectionManager(
				ioReactor);
		cm.setMaxTotal(getPoolSize());

		HttpHost splunk = new HttpHost(getHost(), getPort());
		cm.setMaxPerRoute(new HttpRoute(splunk), getPoolSize());

		httpClient = HttpAsyncClients.custom().setConnectionManager(cm).build();

		uri = new URIBuilder().setScheme(getHTTPs() ? "https" : "http")
				.setHost(getHost()).setPort(getPort())
				.setPath("/services/collector").build();

		httpClient.start();

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
		String message = event.toString();
		message = wrapMessageInQuotes(message);
		try {

			// could use a JSON Object , but the JSON is so trivial , just
			// building it with a StringBuffer
			StringBuffer json = new StringBuffer();
			json.append("{\"").append("event\":").append(message).append(",\"")
					.append("index\":\"").append(getIndex()).append("\",\"")
					.append("source\":\"").append(getSource()).append("\",\"")
					.append("sourcetype\":\"").append(getSourcetype())
					.append("\"").append("}");

			HttpPost post = new HttpPost(uri);
			post.addHeader("Authorization", "Splunk " + getToken());

			StringEntity requestEntity = new StringEntity(json.toString(),
					ContentType.create("application/json", "UTF-8"));

			post.setEntity(requestEntity);
			httpClient.execute(post, null);

		} catch (Exception e) {

			logger.error("Error sending message via HEC transport : "
					+ e.getMessage());

			// something went wrong , put message on the queue for retry
			enqueue(message);
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

}

package com.splunk.javaagent.jmx.mbean;

public interface HECTransportMXBean {

	// attributes

	// getter
	public long getCurrentQueueSize();

	public String getHost();

	public int getPort();

	public String getToken();

	public boolean getHTTPs();

	public int getPoolSize();

	public String getIndex();

	public String getSource();

	public String getSourcetype();

	public long getMaxQueueSize();

	public boolean getDropEventsOnFullQueue();

	public long getMaxBatchSizeEvents();

	public long getMaxInactiveTimeBeforeBatchFlush();

	public long getMaxBatchSizeBytes();

	public boolean getBatchMode();

	// setter
	public void setHost(String val);

	public void setPort(int val);

	public void setToken(String val);

	public void setHTTPs(boolean val);

	public void setPoolSize(int val);

	public void setIndex(String val);

	public void setSource(String val);

	public void setSourcetype(String val);

	public void setMaxQueueSize(long val);

	public void setDropEventsOnFullQueue(boolean val);

	public void setBatchMode(boolean batchMode);

	public void setMaxBatchSizeBytes(long maxBatchSizeBytes);

	public void setMaxBatchSizeEvents(long maxBatchSizeEvents);

	public void setMaxInactiveTimeBeforeBatchFlush(
			long maxInactiveTimeBeforeBatchFlush);

	// operations
	public void start() throws Exception;

	public void stop() throws Exception;

	public void restart() throws Exception;

}

package com.splunk.javaagent.jmx.mbean;

public interface TCPTransportMBean {

	// attributes

	// getter
	public long getCurrentQueueSize();

	public String getHost();

	public int getPort();

	public long getMaxQueueSize();

	public boolean getDropEventsOnFullQueue();

	// setter
	public void setHost(String val);

	public void setPort(int val);

	public void setMaxQueueSize(String val);

	public void setDropEventsOnFullQueue(boolean val);

	// operations
	public void start() throws Exception;

	public void stop() throws Exception;

	public void restart() throws Exception;

}

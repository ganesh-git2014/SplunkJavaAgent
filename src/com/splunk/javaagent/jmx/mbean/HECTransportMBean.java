package com.splunk.javaagent.jmx.mbean;

public interface HECTransportMBean {

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

	// setter
	public void setHost(String val);

	public void setPort(int val);

	public void setToken(String val);

	public void setHTTPs(boolean val);

	public void setPoolSize(int val);

	public void setIndex(String val);

	public void setSource(String val);

	public void setSourcetype(String val);

	public void setMaxQueueSize(String val);

	public void setDropEventsOnFullQueue(boolean val);

	// operations
	public void start() throws Exception;

	public void stop() throws Exception;

	public void restart() throws Exception;

}

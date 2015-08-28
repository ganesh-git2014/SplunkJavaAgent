package com.splunk.javaagent.jmx.mbean;

public interface JavaAgentMXBean {

	// attributes

	// getter
	public String getAppName();

	public String getAppInstance();

	public String getUserEventTags();

	public boolean getStartPaused();

	public String getLoggingLevel();

	public String getTracingBlacklist();

	public String getTracingWhitelist();

	public boolean getTraceMethodEntered();

	public boolean getTraceMethodExited();

	public boolean getTraceClassLoaded();

	public boolean getTraceErrors();

	public boolean getTraceJMX();

	public String getTraceJMXConfigFiles();

	public boolean getTraceHProf();

	public String getTraceHProfTempFile();

	public int getTraceHProfFrequency();

	// setter
	public void setAppName(String val);

	public void setAppInstance(String val);

	public void setUserEventTags(String val);

	public void setLoggingLevel(String val);

	public void setTracingBlacklist(String val);

	public void setTracingWhitelist(String val);

	public void setTraceMethodEntered(boolean val);

	public void setTraceMethodExited(boolean val);

	public void setTraceClassLoaded(boolean val);

	public void setTraceErrors(boolean val);

	public void setTraceJMX(boolean val);

	public void setTraceJMXConfigFiles(String val);

	public void setTraceJMXFrequency(int val);

	public void setTraceHProf(boolean val);

	public void setTraceHProfTempFile(String val);

	public void setTraceHProfFrequency(int val);

	// operations

	public void pause() throws Exception;

	public void unpause() throws Exception;

	public void stopJMX() throws Exception;

	public void startJMX() throws Exception;

	public void stopHprof() throws Exception;

	public void startHprof() throws Exception;

}

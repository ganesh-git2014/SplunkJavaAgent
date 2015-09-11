## Splunk Java Agent v1.2

## Overview


This JVM(Java Virtual Machine)agent can be used to obtain metrics for Java APM(Application Performance Monitoring).

The types of metrics extracted by the agent and streamed to Splunk are :

1. class loading
2. method execution 
3. method timings (cumulative, min, avg, max, std deviation)
4. method call tracing(count of calls, group by app/app node(for clustered systems)/thread/class/package)
5. application/thread stalls
6. errors/exceptions/throwables
7. JVM heap analysis, object/array allocation count/size,class dumps, leak detection, stack traces, frames
8. JMX attributes/operations/notifications from the JVM or Application layer MBean Domains

The agent is able to obtain these metrics dynamically at runtime by "weaving" the necessary bytecode into loaded classes using the Java instrumentation API and the ASM framework.
There is no source code changes required by the end user and no class files on disk are altered.

JMX metrics are obtained via polling MBeans attributes, invoking operations & listening for notifications in the locally running Platform MBeanServer.

JVM heap profiling metrics are obtained via decoding a dynamically generated HPROF dump.

By default , the metrics will be streamed directly into Splunk over TCP, however the transport mechanism is configurable and extensible.

In the Splunk UI you can then create Splunk searches over the agent data and visualizations, reports,alerts etc.. over the results of the searches.
The events are already being fed into Splunk in best practice semantic format, key=value pairs , no additional field extractions are required.
As Splunk is being using to index all the data and perform searches(real time if you wish) massive amounts of tracing data from as many JVMs as you need to 
monitor can be indexed and correlated and you can leverage all of the scalability and HA features of the Splunk platform to deliver an end to end Java APM solution.

You can refer to some search examples in this presentation online : 
http://www.slideshare.net/damiendallimore/splunk-conf-2014-splunking-the-java-virtual-machine


## Supported Java Runtime


1. JRE 5,6,7,8
2. JVMs : Hotspot , JRockit, OpenJDK, IBM J9

## Install


1. Uncompress splunkagent.tar.gz
2. The agent is just a single jar file, splunkagent.jar, you should see this in the uncompressed directory.


## Transports

Data is transported to Splunk via TCP or HEC (HTTP Event Collector)

## Create a TCP input in Splunk

Login to Splunk and browse to Data Inputs to setup a new TCP input.
This is how the agent will by default stream the metrics to Splunk.
Whatever port you designate must be configured in the agent properties also (splunk.transport.tcp.port) 

## Create a HEC input in Splunk

Login to Splunk and browse to Data Inputs to setup a new HEC input.
You can then obtain your HEC token and any other HEC settings to be used in the configuration options detailed below.


## Setup


Pass the follow argument to your JVM at startup:

-javaagent:splunkagent.jar

or 

-javaagent:lib/splunkagent.jar

etc..

The location of the jar file should be relative to the directory where you are executing "java" from.

All dependencies and resources are bundled into the jar file.

## Configuration files


You can configure the agent with the properties file "splunkagent.properties" that resides inside the jar file.
The various options are detailed below.
Open the jar , edit the file, close the jar.

Alternatively you can pass the properties file in as an argument , rather than having to bundle it into the agent jar.

Example : -javaagent:splunkagent.jar=/Users/foo/splunkagent.properties

You can configure the JMX polling with 1 or more config files that reside either inside the jar file at an external location outside of the jar.
The JMX logic is just an embedded version of the "Splunk for JMX" app(http://splunk-base.splunk.com/apps/25505/splunk-for-jmx) , so refer to that app's documentation for config file options.
The names of the JMX config files and the frequency at which they are fired is configured in "splunkagent.properties"

## Dynamic reload

For configuration files that reside outside of the agent jar file , if you make any changes to this file during the JVM runtime, these changes will be automatically detected , reloaded and the agent re initialised without having to perform a JVM restart. Pretty cool huh !

## Tracing verbosity

Unless you want incredibly verbose tracing , you will want to specify just the packages/classes/methods you are interested in profiling in the "agent.whitelist" property

## Logging

Logging levels can be configured in splunkagent.properties.By default logging will get written to a file in the runtime directory named splunkagent.yyyy-MM-dd.log. This gets rolled daily.

## Remote control via JMX

The agent can be completely controlled and introspected dynamically at runtime via JMX, either locally or remotely.
I recommend using JConsole that ships with Java to connect to the JVM's JMX MBeanServer and browse the MBeans in the "splunkjavaagent" domain.
For remote JMX connectivity to your target JVM , you will also need to enable remote JMX.
More info here : http://docs.oracle.com/javase/7/docs/technotes/guides/management/agent.html

## Properties Options

## Core Settings

* agent.app.name : name of the application ie: Tomcat
* agent.app.instance : instance identifier of the application ie: might be a node id in a cluster
* agent.userEventTags : comma delimited list of user defined key=value pairs to add to events sent to Splunk
* agent.startpaused : false | true , if true , the agent will load but not perform any tracing until you unpause it.You can unpause it by editing the properties fille which will get detected and dynamically re initialise the agent or remotely via a JMX operation.
* agent.loggingLevel : ERROR | INFO

## Common Transport Options

* splunk.transport.internalQueueSize : defaults to 10000 events , this the internal memory queue that buffers the events before being sent to Splunk.
* splunk.transport.impl : fully qualified class name, an implementation of the "com.splunk.javaagent.transport.SplunkTransport" interface
* splunk.transport.*.maxQueueSize : defaults to 500K , format [<integer>|<integer>[KB|MB|GB]]
* splunk.transport.*.dropEventsOnQueueFull : true | false , if true then the queue will get emptied when it fills up to accommodate new data.

## TCP Transport

* splunk.transport.tcp.host : Splunk host name, defaults to localhost
* splunk.transport.tcp.port : Splunk TCP port you setup in Splunk Data Inputs


## HEC Transport

* splunk.transport.hec.host : Splunk host name , defaults to localhost
* splunk.transport.hec.port : HEC Port , defaults to 8088
* splunk.transport.hec.token : HEC token
* splunk.transport.hec.https : true | false , defaults to false
* splunk.transport.hec.poolsize : HTTP client connection pool , defaults to 1
* splunk.transport.hec.index : index for the tracing data
* splunk.transport.hec.source : source for the tracing data
* splunk.transport.hec.sourcetype : sourcetypefor the tracing data
* splunk.transport.hec.batchMode : batch upload events vs sending single events , defaults to false
* splunk.transport.hec.maxBatchSizeBytes : will flush a batch upload at this size, defaults to 1MB
* splunk.transport.hec.maxBatchSizeEvents : will flush a batch upload at this number of events, defaults to 100
* splunk.transport.hec.maxInactiveTimeBeforeBatchFlush : will flush a batch upload after this period of inactivity , defaults to 5000 (5 secs)

## Tracing Options

* trace.whitelist : comma delimited string of patterns, see below
* trace.blacklist comma delimited string of patterns, see below
* trace.methodEntered : true | false
* trace.methodExited : true | false
* trace.classLoaded : true | false
* trace.errors : true | false

## HPROF Options

* trace.hprof=true | false
* trace.hprof.file=/etc/tmp/dump.hprof
* trace.hprof.frequency=value in seconds , the frequency at which to generate hprof dumps
* trace.hprof.recordtypes=comma delimited list of HPROF record types to trace.Decimal value of the record tag id (as per the HPROF spec).

## JMX Options

* trace.jmx=true | false
* trace.jmx.configfiles=comma delimited list of XML files(minus the ".xml" suffix) that should reside in the root of splunkagent.jar.Alternatively you can specify a path on the filesystem outside of the jar(minus the ".xml" suffix)
* trace.jmx.default.frequency=value in seconds
* trace.jmx.${configfile}.frequency=value in seconds , optionally you may declare each config file to fire at differing frequencys

## Whitelist/Blacklist Patterns


* Partial package name : com/splunk/
* Full package name : com/splunk/javaagent/test/
* Fully qualified class : com/splunk/javaagent/test/MyClass
* Fully qualified class and method : com/splunk/javaagent/test/MyClass:someMethod

## Contact

This project was initiated by Damien Dallimore
<table>

<tr>
<td><em>Email</em></td>
<td>ddallimore@splunk.com</td>
</tr>

<tr>
<td><em>Twitter</em>
<td>@damiendallimore</td>
</tr>


</table>


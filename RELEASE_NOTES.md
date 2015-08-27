1.2
---
HEC (HTTP Event Collector) transport output option (preferably to TCP)
Dynamic config file reload for external config files , no JVM restart required
Fully JMX enabled(attributes and operations) for remote control and introspection of running agent
"startPaused" mode allows you to load the agent when the JVM starts but not run it until you explicitly unpause it via a JMX remote control operation
Added much more thorough logging

1.1
---
Can now locate jmx config files outside of the agent jar file on the filesystem

1.0
---
Support for Java 7/8 , specifically stackmap frames.
Can optionally pass the properties file in as an argument , rather than having to bundle it into the agent jar
Example : -javaagent:splunkagent.jar=/Users/foo/splunkagent.properties

0.5
---
Initial beta release

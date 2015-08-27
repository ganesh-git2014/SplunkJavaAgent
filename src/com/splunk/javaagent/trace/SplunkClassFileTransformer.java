package com.splunk.javaagent.trace;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import com.splunk.javaagent.SplunkJavaAgent;

public class SplunkClassFileTransformer implements ClassFileTransformer {

	private static Logger logger = Logger
			.getLogger(SplunkClassFileTransformer.class);

	public SplunkClassFileTransformer() {
	}

	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classFileBuffer) throws IllegalClassFormatException {

		boolean proceed = true;

		try {
			// a hacky test to ensure that the class being instrumented
			// can see the required SplunkJavaAgent classes
			loader.loadClass("com.splunk.javaagent.SplunkLogEvent");
		} catch (ClassNotFoundException e) {
			proceed = false;
			logger.error("Class " + className
					+ " can not see the classes in the Agent classloader");
		}

		if (proceed) {

			if (!SplunkJavaAgent.isBlackListed(className)
					&& SplunkJavaAgent.isWhiteListed(className))
				return processClass(className, classBeingRedefined,
						classFileBuffer);
			else
				return classFileBuffer;
		} else {
			return classFileBuffer;
		}
	}

	private byte[] processClass(String className, Class classBeingRedefined,
			byte[] classFileBuffer) {

		SplunkJavaAgent.classLoaded(className);
		ClassReader cr = new ClassReader(classFileBuffer);
		ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
		ClassTracerAdaptor ca = new ClassTracerAdaptor(cw);
		cr.accept(ca, 0);

		return cw.toByteArray();

	}

}

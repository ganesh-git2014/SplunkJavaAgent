package com.splunk.javaagent.trace;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public class MethodTracerAdaptor extends AdviceAdapter {

	private String cName;
	private String mName;
	private String desc;

	public MethodTracerAdaptor(String owner, String name, MethodVisitor mv,
			String desc, int access) {

		super(Opcodes.ASM5, mv, access, name, desc);
		this.mName = name;
		this.mv = mv;
		this.cName = owner;
		this.desc = desc;

	}

	
	@Override
	public void visitCode() {
		try {

			super.visitCode();

			super.visitLdcInsn(cName);
			super.visitLdcInsn(mName);
			super.visitLdcInsn(desc);
			super.visitMethodInsn(Opcodes.INVOKESTATIC,
					"com/splunk/javaagent/SplunkJavaAgent", "methodEntered",
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",false);

			/**
			Type[] paramTypes = Type.getArgumentTypes(desc);
			int paramLength = paramTypes.length;

			// Create array with length equal to number of parameters
			super.visitIntInsn(Opcodes.BIPUSH, paramLength);
			super.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
			super.visitVarInsn(Opcodes.ASTORE, paramLength);

			// Fill the created array with method parameters
			int i = 0;
			for (Type tp : paramTypes) {
				super.visitVarInsn(Opcodes.ALOAD, paramLength);
				super.visitIntInsn(Opcodes.BIPUSH, i);

				if (tp.equals(Type.BOOLEAN_TYPE)) {
					super.visitVarInsn(Opcodes.ILOAD, i);
					super.visitMethodInsn(Opcodes.INVOKESTATIC,
							"java/lang/Boolean", "valueOf",
							"(Z)Ljava/lang/Boolean;");
				} else if (tp.equals(Type.BYTE_TYPE)) {
					super.visitVarInsn(Opcodes.ILOAD, i);
					super.visitMethodInsn(Opcodes.INVOKESTATIC,
							"java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
				} else if (tp.equals(Type.CHAR_TYPE)) {
					super.visitVarInsn(Opcodes.ILOAD, i);
					super.visitMethodInsn(Opcodes.INVOKESTATIC,
							"java/lang/Character", "valueOf",
							"(C)Ljava/lang/Character;");
				} else if (tp.equals(Type.SHORT_TYPE)) {
					super.visitVarInsn(Opcodes.ILOAD, i);
					super.visitMethodInsn(Opcodes.INVOKESTATIC,
							"java/lang/Short", "valueOf",
							"(S)Ljava/lang/Short;");
				} else if (tp.equals(Type.INT_TYPE)) {
					mv.visitVarInsn(Opcodes.ILOAD, i);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC,
							"java/lang/Integer", "valueOf",
							"(I)Ljava/lang/Integer;");
				} else if (tp.equals(Type.LONG_TYPE)) {
					super.visitVarInsn(Opcodes.LLOAD, i);
					super.visitMethodInsn(Opcodes.INVOKESTATIC,
							"java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
					i++;
				} else if (tp.equals(Type.FLOAT_TYPE)) {
					super.visitVarInsn(Opcodes.FLOAD, i);
					super.visitMethodInsn(Opcodes.INVOKESTATIC,
							"java/lang/Float", "valueOf",
							"(F)Ljava/lang/Float;");
				} else if (tp.equals(Type.DOUBLE_TYPE)) {
					super.visitVarInsn(Opcodes.DLOAD, i);
					super.visitMethodInsn(Opcodes.INVOKESTATIC,
							"java/lang/Double", "valueOf",
							"(D)Ljava/lang/Double;");
					i++;
				} else
					super.visitVarInsn(Opcodes.ALOAD, i);

				super.visitInsn(Opcodes.AASTORE);
				i++;
			}

			// Load class name and method name
			super.visitLdcInsn(this.cName);
			super.visitLdcInsn(this.mName);
			super.visitLdcInsn(this.desc);
			// Load the array of parameters that we created
			super.visitVarInsn(Opcodes.ALOAD, paramLength);

			super.visitMethodInsn(Opcodes.INVOKESTATIC,
					"com/splunk/javaagent/SplunkJavaAgent",
					"captureMethodParameterValues",
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V");
            **/
			
		} catch (Exception e) {
		}
	}

	@Override
	public void visitInsn(int opcode) {

		try {

			if (opcode == Opcodes.ATHROW) {
				// get the Throwable object off the stack
				super.visitInsn(Opcodes.DUP);

				int exceptionVar = newLocal(Type.getType(Throwable.class));
				super.visitVarInsn(Opcodes.ASTORE, exceptionVar);

				super.visitLdcInsn(cName);
				super.visitLdcInsn(mName);
				super.visitLdcInsn(desc);
				super.visitVarInsn(ALOAD, exceptionVar);

				super.visitMethodInsn(
						Opcodes.INVOKESTATIC,
						"com/splunk/javaagent/SplunkJavaAgent",
						"throwableCaught",
						"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V",false);
			}

			if (opcode == Opcodes.IRETURN || opcode == Opcodes.FRETURN
					|| opcode == Opcodes.RETURN || opcode == Opcodes.ARETURN
					|| opcode == Opcodes.LRETURN || opcode == Opcodes.DRETURN) {

				super.visitLdcInsn(cName);
				super.visitLdcInsn(mName);
				super.visitLdcInsn(desc);
				super.visitMethodInsn(Opcodes.INVOKESTATIC,
						"com/splunk/javaagent/SplunkJavaAgent", "methodExited",
						"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",false);
			}

			super.visitInsn(opcode);

		} catch (Exception e) {
		}
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {

		// will be overridden by COMPUTE_MAXS
		super.visitMaxs(0, 0);

	}
}

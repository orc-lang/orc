package orc.scalagraalagent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.logging.Logger;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class Agent {
	private static final Logger logger = Logger.getLogger("orc.scalagraalagent");

	public static void premain(String agentArgs, Instrumentation inst) {
		logger.info("Rewriting static \"MODULE$\" to be final for all loaded classes. This helps Graal optimize Scala objects.");
		inst.addTransformer(new ClassFileTransformer() {
			@Override
			public byte[] transform(ClassLoader l, String name, Class<?> c, ProtectionDomain d, byte[] b)
					throws IllegalClassFormatException {
				ClassReader cr = new ClassReader(b);
				ClassWriter cw = new ClassWriter(0);
				//TraceClassVisitor t = new TraceClassVisitor(cw, new PrintWriter(System.out));
				ClassVisitor cv = new MarkModulesAsCompileConstant(cw);
				cr.accept(cv, 0);
				return cw.toByteArray();
			}
		});
	}
}

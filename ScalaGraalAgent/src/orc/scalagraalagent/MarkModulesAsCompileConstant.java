package orc.scalagraalagent;

import java.util.logging.Logger;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class MarkModulesAsCompileConstant extends ClassVisitor {
	private static final Logger logger = Logger.getLogger("orc.scalagraalagent");

	private String className;

	public MarkModulesAsCompileConstant(ClassVisitor cv) {
        super(Opcodes.ASM6, cv);
        this.cv = cv;
    }

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.className = name;
		super.visit(version, access, name, signature, superName, interfaces);
	}

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
    	if (name.equals("MODULE$") && (access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
        	FieldVisitor fv = super.visitField(access | Opcodes.ACC_FINAL, name, descriptor, signature, value);

        	logger.fine(() -> "Set " + this.className.replace('/', '.') + "." + name + " as final.");

        	return fv;
    	} else {
    		return super.visitField(access, name, descriptor, signature, value);
    	}
    }
}
package orc.ast.oil.type;

import java.lang.reflect.TypeVariable;
import java.util.LinkedList;
import java.util.List;

import orc.error.OrcError;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.type.TypingContext;
import orc.type.java.ClassTycon;

/**
 * A type which refers to a Java class (which we will treat as an Orc type).
 * @author quark, dkitchin
 */
public class ClassType extends Type {

	public String classname;
	
	public ClassType(String classname) {
		this.classname = classname;
	}
	
	@Override
	public orc.type.Type transform(TypingContext ctx) throws TypeException {
		return ctx.resolveClassType(classname);
	}
		
	public String toString() {		
		return classname;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.type.Type#marshal()
	 */
	@Override
	public orc.ast.xml.type.Type marshal() {
		return new orc.ast.xml.type.ClassnameType(classname);
	}	
	
}

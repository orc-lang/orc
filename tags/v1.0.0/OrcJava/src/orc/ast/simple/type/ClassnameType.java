package orc.ast.simple.type;

import java.lang.reflect.TypeVariable;
import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.error.OrcError;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.type.java.ClassTycon;

/**
 * A type which refers to a Java class (which we will treat as an Orc type).
 * 
 * @author quark, dkitchin
 */
public class ClassnameType extends Type {

	public String classname;
	
	public ClassnameType(String classname) {
		this.classname = classname;
	}
	
	@Override
	public orc.ast.oil.type.Type convert(Env<orc.ast.simple.type.TypeVariable> env) throws TypeException {
		return new orc.ast.oil.type.ClassType(classname);
	}
	
	/* (non-Javadoc)
	 * @see orc.ast.simple.type.Type#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Type subst(Type T, FreeTypeVariable X) {
		return this;
	}
		
	public String toString() {		
		return classname;
	}	
	
}

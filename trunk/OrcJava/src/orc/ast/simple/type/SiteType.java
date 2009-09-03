package orc.ast.simple.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.error.OrcError;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.type.TypeVariable;

/**
 * A syntactic type corresponding to a Java class implementing a type.
 * 
 * In order to convert this to an actual type, the Java class must be
 * a subtype of orc.type.Type
 * 
 * @author dkitchin
 *
 */
public class SiteType extends Type {

	public String classname;
	
	public SiteType(String classname) {
		this.classname = classname;
	}
	
	@Override
	public orc.ast.oil.type.Type convert(Env<orc.ast.simple.type.TypeVariable> env) {
		return new orc.ast.oil.type.SiteType(classname);
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

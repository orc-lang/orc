package orc.ast.oil.type;

import java.util.LinkedList;
import java.util.List;

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
	public orc.type.Type transform() {
		// FIXME: resolve site types
		return orc.type.Type.BOT;
	}
		
	public String toString() {		
		return classname;
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.type.Type#marshal()
	 */
	@Override
	public orc.ast.xml.type.Type marshal() {
		return new orc.ast.xml.type.SiteType(classname);
	}	
	
}

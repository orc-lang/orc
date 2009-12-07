package orc.ast.oil.type;

import java.util.LinkedList;
import java.util.List;

import orc.error.OrcError;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.type.TypeVariable;
import orc.type.TypingContext;

/**
 * A type corresponding to a Java class which subclasses orc.type.Type,
 * so that it can be instantiated as an external Orc type by the typechecker.
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
	public orc.type.Type transform(TypingContext ctx) throws TypeException {
		return ctx.resolveSiteType(classname);
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

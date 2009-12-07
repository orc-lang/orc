package orc.type.ground;

import orc.error.compiletime.typing.TypeException;
import orc.type.Type;

/**
 * 
 * The Top type. Supertype of all other types.
 * 
 * All other types extend this type, so that we can use the Java
 * inheritance hierarchy to maintain a default subtyping relation.
 * 
 * The Top type can be ascribed to all values, and thus
 * necessarily carries no information.
 * 
 * @author dkitchin
 *
 */
public final class Top extends Type {
	
	public boolean subtype(Type that) throws TypeException {
		return that.isTop();
	}
	
	public boolean isTop() {
		return true;
	}
	
	public String toString() { return "Top"; }
	
	@Override
	public orc.ast.xml.type.Type marshal() {
		return new orc.ast.xml.type.Top();
	}
}

package orc.ast.simple.type;

import orc.env.Env;

/**
 * The syntactic type '_', a placeholder for an unknown type.
 * 
 * @author dkitchin
 *
 */
public class Blank extends Type {

	@Override
	public orc.ast.oil.type.Type convert(Env<TypeVariable> env) {
		return orc.ast.oil.type.Type.BLANK;
	}

	/* (non-Javadoc)
	 * @see orc.ast.simple.type.Type#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Type subst(Type T, FreeTypeVariable X) {
		return this;
	}
	
	public String toString() { return "_"; }
}

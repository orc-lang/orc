package orc.ast.simple.type;

import orc.env.Env;

/**
 * The type 'Bot', subtype of all other types.
 * 
 * @author dkitchin
 *
 */
public class Bot extends Type {

	@Override
	public orc.ast.oil.type.Type convert(Env<TypeVariable> env) {
		return orc.ast.oil.type.Type.BOT;
	}
	
	/* (non-Javadoc)
	 * @see orc.ast.simple.type.Type#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Type subst(Type T, FreeTypeVariable X) {
		return this;
	}

	public String toString() { return "Bot"; }
}

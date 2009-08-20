package orc.ast.extended.type;

import orc.env.Env;

/**
 * The syntactic type 'Bot', subtype of all other types.
 * 
 * @author dkitchin
 *
 */
public class Bot extends Type {

	@Override
	public orc.type.Type convert(Env<String> env) {
		return orc.type.Type.BOT;
	}

	public String toString() { return "Bot"; }
}

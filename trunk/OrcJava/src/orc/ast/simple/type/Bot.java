package orc.ast.simple.type;

import orc.env.Env;

/**
 * The syntactic type 'Bot'.
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

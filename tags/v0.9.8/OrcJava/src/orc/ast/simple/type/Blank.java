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
	public orc.type.Type convert(Env<String> env) {
		return orc.type.Type.BLANK;
	}

	public String toString() { return "_"; }
}

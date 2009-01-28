package orc.ast.simple.type;

import orc.env.Env;

/**
 * The syntactic type 'Integer'.
 * 
 * @author dkitchin
 *
 */
public class IntegerType extends Type {

	@Override
	public orc.type.Type convert(Env<String> env) {
		return orc.type.Type.INTEGER;
	}

	public String toString() { return "Integer"; }
}

package orc.ast.simple.type;

import orc.env.Env;

/**
 * The syntactic type 'Boolean'.
 * 
 * @author dkitchin
 *
 */
public class BooleanType extends Type {

	@Override
	public orc.type.Type convert(Env<String> env) {
		return orc.type.Type.BOOLEAN;
	}

	public String toString() { return "Boolean"; }
}

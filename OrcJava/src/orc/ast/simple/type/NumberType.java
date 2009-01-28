package orc.ast.simple.type;

import orc.env.Env;

/**
 * The syntactic type 'Number'.
 * 
 * @author dkitchin
 *
 */
public class NumberType extends Type {

	@Override
	public orc.type.Type convert(Env<String> env) {
		return orc.type.Type.NUMBER;
	}

	public String toString() { return "Number"; }
}

package orc.ast.simple.type;

import orc.env.Env;

/**
 * The syntactic type 'String'.
 * 
 * @author dkitchin
 *
 */
public class StringType extends Type {

	@Override
	public orc.type.Type convert(Env<String> env) {
		return orc.type.Type.STRING;
	}
	
	public String toString() { return "String"; }
}

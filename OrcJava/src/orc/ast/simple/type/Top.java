package orc.ast.simple.type;

import orc.env.Env;

/**
 * The syntactic type 'Top', supertype of all other types.
 * 
 * @author dkitchin
 *
 */
public class Top extends Type {

	@Override
	public orc.type.Type convert(Env<String> env) {
		return orc.type.Type.TOP;
	}

	public String toString() { return "Top"; }
}

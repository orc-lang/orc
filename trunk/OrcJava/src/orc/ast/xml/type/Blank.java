package orc.ast.xml.type;

import orc.env.Env;

/**
 * The syntactic type '_', a placeholder for an unknown type.
 * @author quark, dkitchin
 */
public class Blank extends Type {
	@Override
	public orc.ast.oil.type.Type unmarshal() {
		return orc.ast.oil.type.Type.BLANK;
	}
}

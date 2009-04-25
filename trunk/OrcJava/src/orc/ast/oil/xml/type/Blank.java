package orc.ast.oil.xml.type;

import orc.Config;
import orc.env.Env;

/**
 * The syntactic type '_', a placeholder for an unknown type.
 * @author quark
 */
public class Blank extends Type {
	@Override
	public orc.type.Type unmarshal(Config config) {
		return orc.type.Type.BLANK;
	}
}

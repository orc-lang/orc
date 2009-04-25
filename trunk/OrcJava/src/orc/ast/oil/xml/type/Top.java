package orc.ast.oil.xml.type;

import orc.Config;
import orc.env.Env;

public class Top extends Type {

	@Override
	public orc.type.Type unmarshal(Config config) {
		return orc.type.Type.TOP;
	}
}

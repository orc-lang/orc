package orc.ast.xml.type;

import orc.env.Env;

public class Bot extends Type {

	@Override
	public orc.ast.oil.type.Type unmarshal() {
		return orc.ast.oil.type.Type.BOT;
	}
}



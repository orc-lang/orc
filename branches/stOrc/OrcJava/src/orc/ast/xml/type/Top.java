package orc.ast.xml.type;

import orc.env.Env;

public class Top extends Type {

	public Top() {}
	
	@Override
	public orc.ast.oil.type.Type unmarshal() {
		return orc.ast.oil.type.Type.TOP;
	}
}
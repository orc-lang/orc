package orc.ast.extended;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.Site;

public class NilExpr extends Expression {

	protected static Argument NIL = new Site(new orc.runtime.sites.core.Nil());
	
	public orc.ast.simple.Expression simplify() {
		
		return new orc.ast.simple.Call(NIL);
	}

}

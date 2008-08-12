package orc.ast.extended;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.Site;

public class NilExpr extends Expression {
	
	public orc.ast.simple.Expression simplify() {
		
		return new orc.ast.simple.Call(new Site(orc.ast.sites.Site.NIL));
	}

	public String toString() {
		return "[]";
	} 
}

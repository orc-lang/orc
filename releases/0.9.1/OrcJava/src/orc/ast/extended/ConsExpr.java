package orc.ast.extended;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.Site;
import orc.ast.simple.arg.Var;

public class ConsExpr extends Expression {

	Expression h;
	Expression t;
	
	public ConsExpr(Expression h, Expression t) {
		this.h = h;
		this.t = t;
	}

	public orc.ast.simple.Expression simplify() {
		
		Var vh = new Var();
		Var vt = new Var();
		
		orc.ast.simple.Expression body = new orc.ast.simple.Call(new Site(orc.ast.sites.Site.CONS), vh, vt);
		
		body = new orc.ast.simple.Where(body, h.simplify(), vh);
		body = new orc.ast.simple.Where(body, t.simplify(), vt);
		
		return body;
	}

}

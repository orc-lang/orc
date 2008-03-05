package orc.ast.extended;

import orc.ast.simple.Expression;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;

public class ValDeclaration implements Declaration {

	String name;
	orc.ast.extended.Expression source;
	
	public ValDeclaration(String name, orc.ast.extended.Expression source) {
		this.name = name;
		this.source = source;
	}

	
	public Expression bindto(Expression target) {
		
		Var a = new Var();
		NamedVar x = new NamedVar(name);
		Expression newsource = source.simplify();
		Expression newtarget = target.subst(a,x);
		
		return new orc.ast.simple.Where(newtarget, newsource, a);
	}




}

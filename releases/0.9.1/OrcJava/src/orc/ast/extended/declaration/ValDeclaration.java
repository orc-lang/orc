package orc.ast.extended.declaration;

import orc.ast.extended.Expression;
import orc.ast.extended.pattern.Pattern;
import orc.ast.simple.arg.Var;

public class ValDeclaration implements Declaration {

	Pattern p;
	Expression f;
	
	public ValDeclaration(Pattern p, Expression f) {
		this.p = p;
		this.f = f;
	}

	
	public orc.ast.simple.Expression bindto(orc.ast.simple.Expression target) {
		
		orc.ast.simple.Expression source = f.simplify();
		Var t = new Var();
		
		source = Pattern.filter(p.match(source));
		target = p.bind(t, target);
		
		return new orc.ast.simple.Where(target, source, t);
	}




}

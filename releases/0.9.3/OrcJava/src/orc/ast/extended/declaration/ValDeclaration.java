package orc.ast.extended.declaration;

import orc.ast.extended.Expression;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.PatternVisitor;
import orc.ast.simple.arg.Var;
import orc.error.compiletime.CompilationException;

public class ValDeclaration implements Declaration {

	Pattern p;
	Expression f;
	
	public ValDeclaration(Pattern p, Expression f) {
		this.p = p;
		this.f = f;
	}

	
	public orc.ast.simple.Expression bindto(orc.ast.simple.Expression target) throws CompilationException {
		
		orc.ast.simple.Expression source = f.simplify();
		
		Var s = new Var();
		Var t = new Var();
		
		PatternVisitor pv = p.process(s);
		
		source = new orc.ast.simple.Sequential(source, pv.filter(), s);
		target = pv.target(t, target);
		
		return new orc.ast.simple.Where(target, source, t);
	}

	public String toString() {
		return "val " + p + " = " + f;
	}
}

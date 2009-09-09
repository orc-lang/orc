package orc.ast.extended.declaration;

import orc.ast.extended.Visitor;
import orc.ast.extended.expression.Expression;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.PatternSimplifier;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.CompilationException;

public class ValDeclaration extends Declaration {

	public Pattern p;
	public Expression f;
	
	public ValDeclaration(Pattern p, Expression f) {
		this.p = p;
		this.f = f;
	}

	
	public orc.ast.simple.expression.Expression bindto(orc.ast.simple.expression.Expression target) throws CompilationException {
		
		orc.ast.simple.expression.Expression source = f.simplify();
		
		Variable s = new Variable();
		Variable t = new Variable();
		
		PatternSimplifier pv = p.process(s);
		
		source = new orc.ast.simple.expression.Sequential(source, pv.filter(), s);
		target = pv.target(t, target);
		
		return new WithLocation(
				new orc.ast.simple.expression.Pruning(target, source, t),
				getSourceLocation());
	}

	public String toString() {
		return "val " + p + " = " + f;
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}

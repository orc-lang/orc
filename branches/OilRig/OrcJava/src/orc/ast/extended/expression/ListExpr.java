package orc.ast.extended.expression;

import java.util.List;

import orc.ast.extended.Visitor;
import orc.ast.simple.argument.Site;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.Call;
import orc.ast.simple.expression.Sequential;
import orc.ast.simple.expression.Pruning;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.CompilationException;
import orc.runtime.ReverseListIterator;


public class ListExpr extends Expression {

	public List<Expression> es;
	
	public ListExpr(List<Expression> es) {
		this.es = es;
	}

	public orc.ast.simple.expression.Expression simplify() throws CompilationException {
		orc.ast.simple.expression.Expression rest = new Call(new Site(orc.ast.sites.Site.NIL));
		ReverseListIterator<Expression> it = new ReverseListIterator<Expression>(es);
		while (it.hasNext()) {
			orc.ast.simple.expression.Expression head = it.next().simplify();
			Variable h = new Variable();
			Variable r = new Variable();
			// rest >r> (Cons(h,r) <h< head)
			rest = new Sequential(
				rest,
				new Pruning(
					new Call(new Site(orc.ast.sites.Site.CONS), h, r),
					head,
					h),
				r);
		}
		return new WithLocation(rest, getSourceLocation());
	}

	public String toString() {
		return "[" + join(es, ", ") + "]";
	}	

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}

package orc.ast.extended;

import java.util.List;

import orc.ast.simple.Call;
import orc.ast.simple.Sequential;
import orc.ast.simple.Where;
import orc.ast.simple.WithLocation;
import orc.ast.simple.arg.Site;
import orc.ast.simple.arg.Var;
import orc.error.compiletime.CompilationException;
import orc.runtime.ReverseListIterator;


public class ListExpr extends Expression {

	List<Expression> es;
	
	public ListExpr(List<Expression> es) {
		this.es = es;
	}

	public orc.ast.simple.Expression simplify() throws CompilationException {
		orc.ast.simple.Expression rest = new Call(new Site(orc.ast.sites.Site.NIL));
		ReverseListIterator<Expression> it = new ReverseListIterator<Expression>(es);
		while (it.hasNext()) {
			orc.ast.simple.Expression head = it.next().simplify();
			Var h = new Var();
			Var r = new Var();
			// rest >r> (Cons(h,r) <h< head)
			rest = new Sequential(
				rest,
				new Where(
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
}

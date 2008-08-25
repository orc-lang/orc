package orc.ast.extended;

import java.util.List;

import orc.ast.simple.WithLocation;
import orc.error.compiletime.CompilationException;


public class ListExpr extends Expression {

	List<Expression> es;
	
	public ListExpr(List<Expression> es) {
		this.es = es;
	}

	public orc.ast.simple.Expression simplify() throws CompilationException {
		
		Expression e = new NilExpr();
		
		for(int i = es.size() - 1; i >= 0; i--) {
			e = new ConsExpr(es.get(i), e);
		}
		
		return new WithLocation(e.simplify(), getSourceLocation());
	}

	public String toString() {
		return "[" + join(es, ", ") + "]";
	}	
}

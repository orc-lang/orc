package orc.ast.extended;

import java.util.List;


public class ListExpr extends Expression {

	List<Expression> es;
	
	public ListExpr(List<Expression> es) {
		this.es = es;
	}

	public orc.ast.simple.Expression simplify() {
		
		Expression e = new NilExpr();
		
		for(int i = es.size() - 1; i >= 0; i--) {
			e = new ConsExpr(es.get(i), e);
		}
		
		return e.simplify();
	}

	
}

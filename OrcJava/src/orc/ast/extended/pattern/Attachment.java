package orc.ast.extended.pattern;

import orc.ast.extended.type.Type;
import orc.ast.simple.argument.Var;
import orc.ast.simple.expression.Expression;
import orc.ast.simple.expression.HasType;
import orc.ast.simple.expression.Pruning;

public class Attachment {

		public Var v;
		public Expression e;
		
		public Attachment(Var v, Expression e) {
			this.v = v;
			this.e = e;
		}

		
		public Expression attach(Expression f) {
			return attach(f, null);
		}
		
		public Expression attach(Expression f, Type t) {
			
			Expression g = e;
			if (t != null) { g = new HasType(g, t, true); }
			
			return new Pruning(f, g, v);
		}
		
}

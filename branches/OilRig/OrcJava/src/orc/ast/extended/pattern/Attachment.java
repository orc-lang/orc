package orc.ast.extended.pattern;

import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.Expression;
import orc.ast.simple.expression.HasType;
import orc.ast.simple.expression.Pruning;
import orc.ast.simple.type.Type;

public class Attachment {

		public Variable v;
		public Expression e;
		
		public Attachment(Variable v, Expression e) {
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

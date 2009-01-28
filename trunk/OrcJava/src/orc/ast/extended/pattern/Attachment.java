package orc.ast.extended.pattern;

import orc.ast.simple.Expression;
import orc.ast.simple.HasType;
import orc.ast.simple.Where;
import orc.ast.simple.arg.Var;
import orc.ast.simple.type.Type;

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
			
			return new Where(f, g, v);
		}
		
}

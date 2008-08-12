package orc.ast.extended.pattern;

import orc.ast.simple.Expression;
import orc.ast.simple.Where;
import orc.ast.simple.arg.Var;

public class Attachment {

		public Var v;
		public Expression e;
		
		public Attachment(Var v, Expression e) {
			this.v = v;
			this.e = e;
		}

		public Expression attach(Expression f) {
			return new Where(f, e, v);
		}
		
}

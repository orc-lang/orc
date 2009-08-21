package orc.ast.extended.pattern;

import java.util.LinkedList;
import java.util.List;

import orc.ast.extended.Visitor;
import orc.ast.simple.argument.*;
import orc.ast.simple.expression.Call;
import orc.ast.simple.expression.Expression;
import orc.ast.simple.expression.Pruning;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.PatternException;

public class TuplePattern extends Pattern {

	public List<Pattern> args;
	
	public TuplePattern(List<Pattern> args) {
		this.args = args;
	}
	
	public TuplePattern() {
		this.args = new LinkedList<Pattern>();
	}
	
//	public Expression bind(Var u, Expression g) {
//		
//		for(int i = 0; i < args.size(); i++) {
//			Pattern p = args.get(i);
//			Expression ui = new Call(u, new Constant(i));
//			g = p.bind(ui, g);
//		}
//		
//		return g;
//	}
//
//	public Expression match(Var u) {
//	
//		// lift(..., pi.match( u(i) ) ,...) 
//		List<Expression> es = new LinkedList<Expression>();
//		for(int i = 0; i < args.size(); i++) {
//			Pattern p = args.get(i);
//			Expression ui = new Call(u, new Constant(i));
//			es.add(p.match(ui));
//		}
//		Expression liftExpr = Pattern.lift(es);
//		
//		// u.fits
//		Expression sizeExpr = new Call(u, new Field("fits"));
//		
//		// u.fits(n), where n is the tuple pattern size
//		Var s = new Var();
//		Argument n = new Constant(args.size());
//		Expression fitsExpr = new Where(new Call(s, n), sizeExpr, s);
//		
//		// if u.fits(n) then lift(...) else none()
//		return Pattern.ifexp(fitsExpr, liftExpr, new Call(Pattern.NONE)); 
//	}

	public boolean strict() {
		return true;
	}

	@Override
	public void process(Variable fragment, PatternSimplifier visitor)
			throws PatternException {
		
		Variable test = new Variable();
		visitor.assign(test, new WithLocation(
				Pattern.trysize(fragment, args.size()),
				getSourceLocation()));
		visitor.require(test);
		
		for (int i = 0; i < args.size(); i++) {
			Pattern p = args.get(i);
			Variable element = new Variable();
			visitor.assign(element, Pattern.nth(fragment, i));
			p.process(element, visitor);
		}
		
	}
	
	public String toString() {
		return "("+orc.ast.extended.expression.Expression.join(args, ", ")+")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}

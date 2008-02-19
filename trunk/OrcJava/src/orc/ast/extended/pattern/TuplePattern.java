package orc.ast.extended.pattern;

import java.util.LinkedList;
import java.util.List;

import orc.ast.simple.Expression;
import orc.ast.simple.Let;
import orc.ast.simple.arg.*;

public class TuplePattern implements Pattern {

	List<Pattern> args;
	
	public TuplePattern(List<Pattern> args) {
		this.args = args;
	}
	
	public Expression bind(Expression g, Var t) {
		
		Expression h = g;
		
		for(int i = 0; i < args.size(); i++) {
			Pattern p = args.get(i);
			Var u = new Var();
			Expression ti = new orc.ast.simple.Call(t, new Constant(i));
			
			h = p.bind(h, u);
			h = new orc.ast.simple.Where(h, ti, u);
		}
		
		return h;
	}

	public Expression match(Expression f) {
	
		List<Argument> letargs = new LinkedList<Argument>();
		Expression h = new Let(letargs);
		Var t = new Var();
		
		for(int i = 0; i < args.size(); i++) {
			Pattern p = args.get(i);
			Var u = new Var();
			Expression ti = new orc.ast.simple.Call(t, new Constant(i));
			
			letargs.add(u);
			
			h = new orc.ast.simple.Where(h, p.match(ti), u);
		}
		
		h = new orc.ast.simple.Sequential(f, h, t);
		
		return h; 
	}

	public boolean strict() {
		return true;
	}

}

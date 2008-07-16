package orc.ast.extended;

import java.util.LinkedList;
import java.util.List;

import orc.ast.extended.pattern.Pattern;
import orc.ast.simple.arg.Constant;
import orc.ast.simple.arg.Var;
import orc.ast.simple.Expression;

public class Clause {

	public List<Pattern> ps;
	public orc.ast.extended.Expression body;
	
	public Clause(List<Pattern> ps, orc.ast.extended.Expression body) {
		this.ps = ps;
		this.body = body;
	}

	public orc.ast.simple.Expression simplify(List<Var> formals, orc.ast.simple.Expression otherwise) {
		
		// Capture the match expression for each strict pattern; lenient patterns are ignored.
		List<Expression> matches = new LinkedList<Expression>();
		
		for (int i = 0; i < ps.size(); i++) {	
			Pattern p = ps.get(i);
			Var t = formals.get(i);
	 
			if (p.strict()) {
				matches.add(p.match(t));
			}
		}
		
		// If no pattern is strict, shortcut the process.
		if (matches.isEmpty()) {
			
			Expression bindExpr = body.simplify();
			
			for (int i = 0; i < ps.size(); i++) {	
				Pattern p = ps.get(i);
				Var t = formals.get(i);
				bindExpr = p.bind(t, bindExpr);
			}
			
			return bindExpr; 
		}
	
		// Lift over all of the strict patterns
		Expression matchExpr = Pattern.lift(matches);
		
		
		// The overall option result of the lift
		Var r = new Var();
		
		
		// This will be bound to the tuple of match results if the match succeeds
		Var u = new Var();
		
		
		// Bind all of the patterns in the clause body
		// If a pattern is strict, its binding is sourced from the tuple of match results.
		// If a pattern is not strict, it is simply bound from the corresponding formal.
		Expression bindExpr = body.simplify();
		int uIndex = 0;
		for (int i = 0; i < ps.size(); i++) {	
			Pattern p = ps.get(i);
			Var t = formals.get(i);
	 
			if (p.strict()) {
				// Source directly if there is only one strict match
				if (matches.size() == 1) {
					bindExpr = p.bind(u, bindExpr);
				}
				// Otherwise, source from a tuple
				else {
					Expression ui = new orc.ast.simple.Call(u, new Constant(uIndex++));
					bindExpr = p.bind(ui, bindExpr);
				}
				
			}
			else {
				bindExpr = p.bind(t, bindExpr);
			}
		}
		
		
		// Success branch.
		// isSome(r) >u> ...binds...
		Expression sbranch = new orc.ast.simple.Sequential(new orc.ast.simple.Call(Pattern.ISSOME, r), bindExpr, u);
		
		// Failure branch.
		// isNone(r) >> ...otherwise...
		Expression nbranch = new orc.ast.simple.Sequential(new orc.ast.simple.Call(Pattern.ISNONE, r), otherwise, new Var());
		
		
		
		// Pipe the match result into these branches
		return new orc.ast.simple.Sequential(matchExpr, new orc.ast.simple.Parallel(sbranch, nbranch), r);
	}
	
	
}

package orc.ast.extended;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import orc.ast.extended.pattern.Attachment;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.PatternVisitor;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.Constant;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.ast.simple.Expression;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.NonlinearPatternException;
import orc.error.compiletime.PatternException;

public class Clause {

	public List<Pattern> ps;
	public orc.ast.extended.Expression body;
	
	public Clause(List<Pattern> ps, orc.ast.extended.Expression body) {
		this.ps = ps;
		this.body = body;
	}

	public orc.ast.simple.Expression simplify(List<Var> formals, orc.ast.simple.Expression otherwise) throws CompilationException {
		
		Expression newbody = body.simplify();
		List<PatternVisitor> stricts = new LinkedList<PatternVisitor>();
				
		Set<NamedVar> allvars = new TreeSet<NamedVar>();
		for(int i = 0; i < ps.size(); i++) {
			Pattern p = ps.get(i);
			Var arg = formals.get(i);
			
			// Push the argument through its matching pattern
			PatternVisitor pv = p.process(arg);
			
			// Let's make sure this pattern didn't duplicate any existing variables
			for (NamedVar x : pv.vars()) {
				if (allvars.contains(x)) {
					throw new NonlinearPatternException(x);
				}
				else {
					allvars.add(x);
				}
			}
			
			// If this is a strict pattern, save its visitor. We'll process it later.
			if (p.strict()) {
				stricts.add(pv); 
			}
			else {
				// Just substitute the argument directly, preserving non-strictness
				newbody = pv.target(arg, newbody);
			}
		}
		
	
		if (stricts.size() > 0) {
			// If any pattern was strict, we need to put in its filter,
			// and check all of the filters to make sure they succeeded.
			
			Var binds = new Var();
			List<Attachment> filters = new LinkedList<Attachment>();
			
			if (stricts.size() == 1) {
				// If there is only one strict pattern, we won't need a result tuple
				PatternVisitor pv = stricts.get(0);
				filters.add(new Attachment(new Var(), pv.filter()));
				newbody = pv.target(binds, newbody);				
			}
			else /* size >= 2 */ { 
				for(int i = 0; i < stricts.size(); i++) {
					// Add this pattern's output as a component of the result tuple
					PatternVisitor pv = stricts.get(i);
					filters.add(new Attachment(new Var(), pv.filter()));
					
					// Pull that output from the result tuple on the other side
					Expression lookup = Pattern.nth(binds, i);
					Var x = new Var();
					newbody = pv.target(x, newbody);				
					newbody = new orc.ast.simple.Where(newbody, lookup, x);
				}
			}
						
			
			Var z = new Var();
			
			/* Build the left hand side */
			Expression caseof = Pattern.caseof(z, binds, newbody, otherwise);
			
			/* Build the right hand side */
			Expression lift;
			if (filters.size() == 1) {
				Attachment a = filters.get(0);
				lift = Pattern.lift(a.v);
				lift = a.attach(lift);
			}
			else {
				
				// First, find all of the variable names and make a list [y1...yn]
				List<Argument> ys = new LinkedList<Argument>();
				for (Attachment a : filters) {
					ys.add(a.v);
				}
				
				/* Then construct: 
				 * 
				 * lift(yall) 
				 * <yall< (y1, ... , yn)
				 * <y1< filter1
				 * ...
				 * <yn< filtern
				 * 
				 */
				Var yall = new Var();
				lift = new orc.ast.simple.Where(Pattern.lift(yall), new orc.ast.simple.Let(ys), yall);
				for (Attachment a : filters) {
					lift = a.attach(lift);
				}
			}
			
			// Now put it all together.
			newbody = new orc.ast.simple.Where(caseof, lift, z);
		}
		else {
			// If there are no strict patterns, it suffices to just use the
			// body as given, since no pattern can fail.
		}
		
		return newbody;
	}
	
	
}

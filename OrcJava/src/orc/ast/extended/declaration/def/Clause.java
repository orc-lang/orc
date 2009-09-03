package orc.ast.extended.declaration.def;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import orc.ast.extended.pattern.Attachment;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.PatternSimplifier;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.Constant;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.Expression;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.NonlinearPatternException;
import orc.error.compiletime.PatternException;

public class Clause {

	public List<Pattern> ps;
	public orc.ast.extended.expression.Expression body;
	
	public Clause(List<Pattern> ps, orc.ast.extended.expression.Expression body) {
		this.ps = ps;
		this.body = body;
	}

	
	/**
	 * Simplify a clause.
	 * 
	 * This creates a new expression which will try to match the given
	 * formals against this clause's patterns. If the match succeeds,
	 * the clause body is executed with the bindings created by the
	 * pattern matches. If the match fails, the otherwise expression
	 * is executed.
	 * 
	 * This function can be applied multiple times in sequence to
	 * make an expression which tries multiple clauses in linear order;
	 * see the translation of clausal definitions for an example.
	 * 
	 * @param formals  The formal arguments against which to match. These are
	 * 				   created elsewhere, by some other translation step.
	 * @param otherwise The alternative expression to execute if the clause does not fire.
	 * @return A new expression which will try the clause, and then run the alternative if
	 * 		   the clause fails to match.
	 * @throws CompilationException
	 */
	public orc.ast.simple.expression.Expression simplify(List<Variable> formals, orc.ast.simple.expression.Expression otherwise) throws CompilationException {

		Expression newbody = body.simplify();
		List<PatternSimplifier> stricts = new LinkedList<PatternSimplifier>();
				
		Set<FreeVariable> allvars = new TreeSet<FreeVariable>();
		for(int i = 0; i < ps.size(); i++) {
			Pattern p = ps.get(i);
			Variable arg = formals.get(i);
			
			// Push the argument through its matching pattern
			PatternSimplifier pv = p.process(arg);
			
			// Let's make sure this pattern didn't duplicate any existing variables
			for (FreeVariable x : pv.vars()) {
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
			
			Variable binds = new Variable();
			List<Attachment> filters = new LinkedList<Attachment>();
			
			if (stricts.size() == 1) {
				// If there is only one strict pattern, we won't need a result tuple
				PatternSimplifier pv = stricts.get(0);
				filters.add(new Attachment(new Variable(), pv.filter()));
				newbody = pv.target(binds, newbody);				
			} else /* size >= 2 */ { 
				for(int i = 0; i < stricts.size(); i++) {
					// Add this pattern's output as a component of the result tuple
					PatternSimplifier pv = stricts.get(i);
					filters.add(new Attachment(new Variable(), pv.filter()));
					
					// Pull that output from the result tuple on the other side
					Expression lookup = Pattern.nth(binds, i);
					Variable x = new Variable();
					newbody = pv.target(x, newbody);				
					newbody = new orc.ast.simple.expression.Pruning(newbody, lookup, x);
				}
			}
						
			
			Variable z = new Variable();
			
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
				Variable yall = new Variable();
				lift = new orc.ast.simple.expression.Pruning(Pattern.lift(yall), new orc.ast.simple.expression.Let(ys), yall);

				for (Attachment a : filters) {
					lift = a.attach(lift);
				}
			}
			
			// Now put it all together.
			newbody = new orc.ast.simple.expression.Pruning(caseof, lift, z);
		}
		else {
			// If there are no strict patterns, it suffices to just use the
			// body as given, since no pattern can fail.
		}
		
		return newbody;
	}
	
	
}

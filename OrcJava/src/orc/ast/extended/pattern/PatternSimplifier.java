package orc.ast.extended.pattern;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import orc.ast.simple.type.Type;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.Expression;
import orc.ast.simple.expression.Let;
import orc.ast.simple.expression.Parallel;
import orc.ast.simple.expression.Sequential;
import orc.ast.simple.expression.Pruning;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.NonlinearPatternException;

/**
 * Used to help simplify patterns.
 * This used to be called PatternVisitor, but it was renamed
 * because it doesn't actually use the visitor pattern and we
 * may want to someday introduce an actual abstract PatternVisitor.
 * 
 * @author dkitchin
 */
public class PatternSimplifier {
	
	List<Argument> requiredVars;
	List<Argument> boundVars;
	Map<FreeVariable, Integer> bindingEntries;
	Map<Variable, Type> ascriptions;
	List<Attachment> attachments;
	
	public PatternSimplifier() 
	{
		this.requiredVars = new LinkedList<Argument>();
		this.boundVars = new LinkedList<Argument>();
		this.bindingEntries = new TreeMap<FreeVariable,Integer>();
		this.attachments = new LinkedList<Attachment>();
		this.ascriptions = new HashMap<Variable, Type>();
	}

	public void assign(Variable s, Expression e) {
		attachments.add(0, new Attachment(s,e));
	}
	
	public void ascribe(Variable s, Type t) {
		ascriptions.put(s, t);
	}
	
	public void subst(Variable s, FreeVariable x) throws NonlinearPatternException {
		
		if (bindingEntries.containsKey(x)) {
			throw new NonlinearPatternException(x);
		}
		else {
			bindingEntries.put(x, bind(s));
		}	
	}
	
	public void require(Variable s) {
		requiredVars.add(s);
	}
	
	private int bind(Variable s) {
		int i = boundVars.indexOf(s);
		
		if (i < 0) {
			i = boundVars.size();
			boundVars.add(s);
		}
		
		return i;
	}
	
	public Set<FreeVariable> vars() {
		return bindingEntries.keySet();
	}
	
	public Expression filter() {
		
		/* 
		 * We require that if only one value is bound, root 
		 * publishes exactly that value, and not a tuple.
		 * Right now, Let enforces that invariant for us.
		 */
		Expression root = new Let(boundVars);
		
		if (requiredVars.size() > 0) {
			Expression required = new Let(requiredVars); 
			root = new Sequential(required, root, new Variable());
		}
		
		for (Attachment a : attachments) {
			root = a.attach(root, ascriptions.get(a.v));
		}
		
		return root;
	}
	
	public Expression target(Variable u, Expression g) {
		
		/* If there is exactly one bound value, u has just that value;
		 * it is not a tuple, so we do not do any lookup.
		 */
		if (boundVars.size() == 1) {
			for (Entry<FreeVariable, Integer> e : bindingEntries.entrySet()) {
				FreeVariable x = e.getKey();
				// we don't use the index since it must be 0.
				g = g.subvar(u, x);
			}
		}
		/* Otherwise, each entry is retrieved with a lookup */
		else {		
			for (Entry<FreeVariable, Integer> e : bindingEntries.entrySet()) {
				FreeVariable x = e.getKey();
				
				int i = e.getValue();
				Expression getter = Pattern.nth(u,i);
				
				Variable v = new Variable();
				g = g.subvar(v, x);
				g = new Pruning(g, getter, v);
			}

		}
		
		return g;
	}
	
}
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

import orc.ast.simple.Expression;
import orc.ast.simple.Let;
import orc.ast.simple.Parallel;
import orc.ast.simple.Sequential;
import orc.ast.simple.Where;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.ast.simple.type.Type;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.NonlinearPatternException;

/**
 * Used to help simplify patterns.
 * This used to be called PatternVisitor, but it was renamed
 * because it doesn't actually use the visitor pattern and we
 * may want to someday introduce an actual abstract PatternVisitor.
 * @author dkitchin
 */
public class PatternSimplifier {
	
	List<Argument> requiredVars;
	List<Argument> boundVars;
	Map<NamedVar, Integer> bindingEntries;
	Map<Var, Type> ascriptions;
	Set<Integer> publishEntries;
	List<Attachment> attachments;
	
	public PatternSimplifier() 
	{
		this.requiredVars = new LinkedList<Argument>();
		this.boundVars = new LinkedList<Argument>();
		this.bindingEntries = new TreeMap<NamedVar,Integer>();
		this.publishEntries = new TreeSet<Integer>();
		this.attachments = new LinkedList<Attachment>();
		this.ascriptions = new HashMap<Var, Type>();
	}

	public void assign(Var s, Expression e) {
		attachments.add(0, new Attachment(s,e));
	}
	
	public void ascribe(Var s, Type t) {
		ascriptions.put(s, t);
	}
	
	public void subst(Var s, NamedVar x) throws NonlinearPatternException {
		
		if (bindingEntries.containsKey(x)) {
			throw new NonlinearPatternException(x);
		}
		else {
			bindingEntries.put(x, bind(s));
		}	
	}
	
	public void require(Var s) {
		requiredVars.add(s);
	}
	
	public void publish(Var s) {
		publishEntries.add(bind(s));
	}

	
	private int bind(Var s) {
		int i = boundVars.indexOf(s);
		
		if (i < 0) {
			i = boundVars.size();
			boundVars.add(s);
		}
		
		return i;
	}
	
	public Set<NamedVar> vars() {
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
			root = new Sequential(required, root, new Var());
		}
		
		for (Attachment a : attachments) {
			root = a.attach(root, ascriptions.get(a.v));
		}
		
		return root;
	}
	
	public Expression target(Var u, Expression g) {
		
		/* If there is exactly one bound value, u has just that value;
		 * it is not a tuple, so we do not do any lookup.
		 */
		if (boundVars.size() == 1) {
			for (Entry<NamedVar, Integer> e : bindingEntries.entrySet()) {
				NamedVar x = e.getKey();
				// we don't use the index since it must be 0.
				g = g.subst(u, x);
			}

			for (Integer i : publishEntries) {
				// we don't use the index since it must be 0.
				Expression getter = new Let(u);
				g = new Parallel(g, getter);
			}
		}
		/* Otherwise, each entry is retrieved with a lookup */
		else {		
			for (Entry<NamedVar, Integer> e : bindingEntries.entrySet()) {
				NamedVar x = e.getKey();
				
				int i = e.getValue();
				Expression getter = Pattern.nth(u,i);
				
				Var v = new Var();
				g = g.subst(v, x);
				g = new Where(g, getter, v);
			}

			for (Integer i : publishEntries) {
				Expression getter = Pattern.nth(u,i);
				g = new Parallel(g, getter);
			}
		}
		
		return g;
	}
	
}
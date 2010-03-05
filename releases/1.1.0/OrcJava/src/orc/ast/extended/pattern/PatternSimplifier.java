//
// PatternSimplifier.java -- Java class PatternSimplifier
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.extended.pattern;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.Expression;
import orc.ast.simple.expression.Let;
import orc.ast.simple.expression.Pruning;
import orc.ast.simple.expression.Sequential;
import orc.ast.simple.type.Type;
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

	public PatternSimplifier() {
		this.requiredVars = new LinkedList<Argument>();
		this.boundVars = new LinkedList<Argument>();
		this.bindingEntries = new TreeMap<FreeVariable, Integer>();
		this.attachments = new LinkedList<Attachment>();
		this.ascriptions = new HashMap<Variable, Type>();
	}

	public void assign(final Variable s, final Expression e) {
		attachments.add(0, new Attachment(s, e));
	}

	public void ascribe(final Variable s, final Type t) {
		ascriptions.put(s, t);
	}

	public void subst(final Variable s, final FreeVariable x) throws NonlinearPatternException {

		if (bindingEntries.containsKey(x)) {
			throw new NonlinearPatternException(x);
		} else {
			bindingEntries.put(x, bind(s));
		}
	}

	public void require(final Variable s) {
		requiredVars.add(s);
	}

	private int bind(final Variable s) {
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
			final Expression required = new Let(requiredVars);
			root = new Sequential(required, root, new Variable());
		}

		for (final Attachment a : attachments) {
			root = a.attach(root, ascriptions.get(a.v));
		}

		return root;
	}

	public Expression target(final Variable u, Expression g) {

		/* If there is exactly one bound value, u has just that value;
		 * it is not a tuple, so we do not do any lookup.
		 */
		if (boundVars.size() == 1) {
			for (final Entry<FreeVariable, Integer> e : bindingEntries.entrySet()) {
				final FreeVariable x = e.getKey();
				// we don't use the index since it must be 0.
				g = g.subvar(u, x);
			}
		}
		/* Otherwise, each entry is retrieved with a lookup */
		else {
			for (final Entry<FreeVariable, Integer> e : bindingEntries.entrySet()) {
				final FreeVariable x = e.getKey();

				final int i = e.getValue();
				final Expression getter = Pattern.nth(u, i);

				final Variable v = new Variable();
				g = g.subvar(v, x);
				g = new Pruning(g, getter, v);
			}

		}

		return g;
	}

}

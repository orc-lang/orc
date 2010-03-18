//
// AstNode.java -- Java class AstNode
// Project OrcJava
//
// $Id$
//
// Created by jthywiss on Mar 17, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import orc.ast.oil.expression.Expression;
import orc.ast.oil.expression.argument.Variable;

/**
 * A node of the portable (.oil, for Orc Intermediate Language) abstract syntax tree.
 * <p>
 * <code>AstNode</code>s and mere node attributes are delineated as follows:
 * An <code>AstNode</code> or its children can have expressions, which evaluate
 * to an Orc value.  <code>AstNode</code> attributes are not expressions, nor
 * do they contain expressions.
 * For example, calls, defs, and constants are <code>AstNode</code>s.
 * Types are not <code>AstNode</code>s, but complex <code>AstNode</code> attributes.
 *
 * @author jthywiss
 */
public abstract class AstNode {
	private transient AstNode parent;
	private static transient Map<Class, List<Field>> nodeTypeAttrMap = new HashMap<Class, List<Field>>();
	private static transient Map<Class, List<Field>> nodeTypeChildMap = new HashMap<Class, List<Field>>();

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	abstract public int hashCode();

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	abstract public boolean equals(Object obj);

	private void initNodeFieldLists() {
		final List<Field> nodeAttrFields = new ArrayList<Field>();
		final List<Field> nodeChildFields = new ArrayList<Field>();
		final Field fields[] = getClass().getFields();
		AccessibleObject.setAccessible(fields, true);
		for (final Field field : fields) {
			sanityCheckChildAnnotation(field);
			if (field.getAnnotation(ChildNode.class) != null) {
				nodeChildFields.add(field);
			}
		}
		nodeTypeAttrMap.put(getClass(), nodeAttrFields);
		nodeTypeChildMap.put(getClass(), nodeChildFields);
	}

	private void sanityCheckChildAnnotation(final Field field) {
		if (field.getAnnotation(ChildNode.class) != null) {
			// Field is annotated
			if ((field.getModifiers() & (Modifier.STATIC | Modifier.TRANSIENT)) == 0) {
				if (AstNode.class.isAssignableFrom(field.getType())) {
					// An AstNode
					return;
				} else if (Collection.class.isAssignableFrom(field.getType())) {
					// Collection of presumed AstNodes
					return; // Can't check element type
				} else if (field.getType().isArray() && AstNode.class.isAssignableFrom(field.getType().getComponentType())) {
					// Array of AstNodes
					return;
				} else {
					throw new AssertionError("Field " + field.getName() + " in " + field.getDeclaringClass() + " isn't an AstNode/collection/array, yet it carries @ChildNode annotation");
				}
			} else {
				throw new AssertionError("Field " + field.getName() + " in " + field.getDeclaringClass() + " is static or transient, yet it carries @ChildNode annotation");
			}
		} else {
			if ((field.getModifiers() & (Modifier.STATIC | Modifier.TRANSIENT)) == 0) {
				if (AstNode.class.isAssignableFrom(field.getType())) {
					// An AstNode
					throw new AssertionError("Field " + field.getName() + " in " + field.getDeclaringClass() + " is an AstNode, yet it does NOT carry @ChildNode annotation");
				} else if (Collection.class.isAssignableFrom(field.getType())) {
					// Collection of AstNodes
					return; // Can't check element type
				} else if (field.getType().isArray() && AstNode.class.isAssignableFrom(field.getType().getComponentType())) {
					// Array of AstNodes
					throw new AssertionError("Field " + field.getName() + " in " + field.getDeclaringClass() + " is an AstNode array, yet it does NOT carry@ChildNode annotation");
				} else {
					return;
				}
			}
		}
	}

	/**
	 * Compare the attributes of two AST nodes, but not references to child AST subtrees.
	 * This differs from {@link #equals(Object)} in that equals does a "deep" comparison.
	 * For example, <code>nodeEquals</code> of two parallel combinators will return true, without evaluating if 
	 * the left and right expressions are equal.
	 * <p>
	 * A node's field is considered a subtree reference if the field is assignable to an {@link AstNode},
	 * is a collection whose elements are assignable to an {@link AstNode}, or.
	 * is a array whose elements are assignable to an {@link AstNode}.
	 * 
	 * @param that the other AST node to compare
	 * @return true if the two nodes' attributes are equal
	 */
	public boolean nodeEquals(final Expression that) {
		if (this == that) {
			return true;
		}
		if (getClass() != that.getClass()) {
			return false;
		}

		if (!nodeTypeAttrMap.containsKey(getClass())) {
			initNodeFieldLists();
		}

		for (final Field field : nodeTypeAttrMap.get(getClass())) {
			try {
				if (field.get(this) == null) {
					if (field.get(that) != null) {
						return false;
					}
				} else if (!field.get(this).equals(field.get(that))) {
					return false;
				}
			} catch (final IllegalAccessException e) {
				// Shouldn't happen, because we setAccessible(true) above 
				throw new AssertionError(e);
			}
		}
		return true;
	}

	/**
	 * Returns a list of AST node children of this node.
	 * <p>
	 * A node's field is considered a subtree reference if the field is assignable to an {@link AstNode},
	 * is a collection whose elements are assignable to an {@link AstNode}, or.
	 * is a array whose elements are assignable to an {@link AstNode}.
	 * 
	 * @return Mutable list of AST node children of this node
	 */
	public List<AstNode> getChildren() {
		if (!nodeTypeChildMap.containsKey(getClass())) {
			initNodeFieldLists();
		}

		final List<AstNode> children = new ArrayList<AstNode>();

		for (final Field field : nodeTypeChildMap.get(getClass())) {
			try {
				final Object childFieldValue = field.get(this);
				// Subtlety: This must test against the field's type, not its value's type
				if (AstNode.class.isAssignableFrom(field.getType())) {
					// An AstNode
					children.add((AstNode) childFieldValue);
				} else if (Collection.class.isAssignableFrom(field.getType())) {
					// Collection of presumed AstNodes
					children.addAll((Collection<AstNode>) childFieldValue);
				} else if (field.getType().isArray() && AstNode.class.isAssignableFrom(field.getType().getComponentType())) {
					// Array of AstNodes
					for (final AstNode child : (AstNode[]) childFieldValue) {
						children.add(child);
					}
				} else {
					// nodeChildFields' elements must match one of the three cases above
					throw new AssertionError("AstNode child is not an AstNode/collection/array: " + field.get(this));
				}
			} catch (final IllegalAccessException e) {
				// Shouldn't happen, because we setAccessible(true) above 
				throw new AssertionError(e);
			}
		}

		return children;
	}

	/**
	 * Initialize the parent references in the children of this node, recursively.
	 */
	public void initParents() {
		for (final AstNode child : getChildren()) {
			child.initParents();
			child.parent = this;
		}
	}

	/**
	 * @return the parent AST node of this node; null at root, or if initParents() not called
	 */
	public AstNode getParent() {
		return parent;
	}

	/**
	 * @return the root of the AST; this if at root, or if initParents() not called
	 */
	public AstNode getRoot() {
		if (parent == null) {
			return this;
		}
		return parent.getRoot();
	}

	/**
	 * Find the set of free variables in this AST subtree. 
	 * 
	 * @return 	The set of free variables.
	 */
	abstract public Set<Variable> freeVars();

	/**
	 * If this AST subtree has any indices which are >= depth,
	 * add (index - depth) to the index set accumulator. The depth 
	 * increases each time this method recurses through a binder.
	 * 
	 * @param indices   The index set accumulator.
	 * @param depth    The minimum index for a free variable.
	 */
	public abstract void addIndices(Set<Integer> indices, int depth);
}

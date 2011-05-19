//
// OrcTreeModelBuilder.java -- Java class OrcTreeModelBuilder
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Aug 5, 2009.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.edit;

import orc.ast.AST;
import orc.ast.ext.ClassImport;
import orc.ast.ext.DefDeclaration;
import orc.ast.ext.Include;
import orc.ast.ext.SiteDeclaration;
import orc.ast.ext.TypeDeclaration;
import orc.ast.ext.Val;

import org.eclipse.imp.editor.ModelTreeNode;
import org.eclipse.imp.services.base.TreeModelBuilderBase;

import scala.collection.JavaConversions;
import scala.util.parsing.input.NoPosition$;

/**
 * Builds an Outline view tree that is a subset of the Orc extended AST
 *
 * @see orc.ast.AST
 * @see org.eclipse.imp.editor.ModelTreeNode
 */
public class OrcTreeModelBuilder extends TreeModelBuilderBase {
	private static final int INCLUDE_CATEGORY = 1;
	private static final int TYPE_DECL_CATEGORY = 2;
	private static final int CALLABLE_DECL_CATEGORY = 3;
	private static final int SIMPLE_VAL_DECL_CATEGORY = 4;

	/* (non-Javadoc)
	 * @see org.eclipse.imp.services.base.TreeModelBuilderBase#visitTree(java.lang.Object)
	 */
	@Override
	public void visitTree(final Object root) {
		if (root == null) {
			return;
		}
		visit(((AST) root));
	}

	private void visit(final AST ast) {
		if (ast.pos() instanceof NoPosition$) {
			return; // NoPosition$ is confusing to the outline control
		}
		/*
		 * Don't forget to update OrcLabelProvider.getImageFor and getLabelFor
		 * to handle new node types in the outline view.
		 */
		if (ast instanceof ClassImport) {
			createSubItem(ast, CALLABLE_DECL_CATEGORY);
		} else if (ast instanceof DefDeclaration) {
			pushSubItem(ast, CALLABLE_DECL_CATEGORY);
		} else if (ast instanceof Include && ((Include) ast).origin() != null && ((Include) ast).origin().length() > 0) {
			createSubItem(ast, INCLUDE_CATEGORY);
		} else if (ast instanceof SiteDeclaration) {
			createSubItem(ast, CALLABLE_DECL_CATEGORY);
		} else if (ast instanceof TypeDeclaration) {
			createSubItem(ast, TYPE_DECL_CATEGORY);
		} else if (ast instanceof Val) {
			createSubItem(ast, SIMPLE_VAL_DECL_CATEGORY);
		}

		for (final AST currChild : JavaConversions.asJavaIterable(ast.subtrees())) {
			visit(currChild);
		}

		if (ast instanceof DefDeclaration) {
			popSubItem();
		}
	}

	/**
	 * Creates a child of the current node in the model tree.
	 * 
	 * @param n AST node to associate with this node in the model tree
	 * @param category integer category of model tree node (currently used for sorting -- sort on category then label text)
	 * @return ModeTreeNode created with current node as parent
	 * @see org.eclipse.imp.services.base.TreeModelBuilderBase#createSubItem(java.lang.Object, int)
	 */
	@Override
	protected ModelTreeNode createSubItem(final Object n, final int category) {
		return super.createSubItem(n, category);
	}

	/**
	 * Creates a child of the current node in the model tree.
	 * 
	 * Equivalent to <code>createSubItem(n, DEFAULT_CATEGORY)</code> 
	 * 
	 * @param n AST node to associate with this node in the model tree
	 * @return ModeTreeNode created with current node as parent
	 * @see org.eclipse.imp.services.base.TreeModelBuilderBase#createSubItem(java.lang.Object)
	 */
	@Override
	protected ModelTreeNode createSubItem(final Object n) {
		return super.createSubItem(n);
	}

	/**
	 * Creates a node for the model tree which has no parent node.
	 * 
	 * @param n AST node to associate with this node in the model tree
	 * @param category integer category of model tree node (currently used for sorting -- sort on category then label text)
	 * @return ModeTreeNode created with no parent
	 * @see org.eclipse.imp.services.base.TreeModelBuilderBase#createTopItem(java.lang.Object, int)
	 */
	@Override
	protected ModelTreeNode createTopItem(final Object n, final int category) {
		return super.createTopItem(n, category);
	}

	/**
	 * Creates a node for the model tree which has no parent node.
	 * 
	 * Equivalent to <code>createTopItem(n, DEFAULT_CATEGORY)</code> 
	 * 
	 * @param n AST node to associate with this node in the model tree
	 * @return ModeTreeNode created with no parent
	 * @see org.eclipse.imp.services.base.TreeModelBuilderBase#createTopItem(java.lang.Object)
	 */
	@Override
	protected ModelTreeNode createTopItem(final Object n) {
		return super.createTopItem(n);
	}

	/**
	 * Change the current node to the parent of the current current node.
	 * 
	 * @see org.eclipse.imp.services.base.TreeModelBuilderBase#popSubItem()
	 */
	@Override
	protected void popSubItem() {
		super.popSubItem();
	}

	/**
	 * Creates a child of the current node in the model tree, then makes it the current node.
	 * Thus, future nodes will be build in this sub-tree, until {@link #popSubItem()} is invoked.
	 * 
	 * @param n AST node to associate with this node in the model tree
	 * @param category integer category of model tree node (currently used for sorting -- sort on category then label text)
	 * @return ModeTreeNode created with previous current node as parent
	 * @see org.eclipse.imp.services.base.TreeModelBuilderBase#pushSubItem(java.lang.Object, int)
	 */
	@Override
	protected ModelTreeNode pushSubItem(final Object n, final int category) {
		return super.pushSubItem(n, category);
	}

	/**
	 * Creates a child of the current node in the model tree, then makes it the current node.
	 * Thus, future nodes will be build in this sub-tree, until {@link #popSubItem()} is invoked.
	 * 
	 * Equivalent to <code>pushSubItem(n, DEFAULT_CATEGORY)</code> 
	 * 
	 * @param n AST node to associate with this node in the model tree
	 * @return ModeTreeNode created with previous current node as parent
	 * @see org.eclipse.imp.services.base.TreeModelBuilderBase#pushSubItem(java.lang.Object)
	 */
	@Override
	protected ModelTreeNode pushSubItem(final Object n) {
		return super.pushSubItem(n);
	}
}

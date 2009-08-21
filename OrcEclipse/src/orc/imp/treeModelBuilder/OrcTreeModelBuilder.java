//
// OrcTreeModelBuilder.java -- Java class OrcTreeModelBuilder
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Aug 5, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.imp.treeModelBuilder;

import orc.ast.extended.ASTNode;
import orc.ast.extended.Walker;
import orc.ast.extended.declaration.ClassDeclaration;
import orc.ast.extended.declaration.SiteDeclaration;
import orc.ast.extended.declaration.ValDeclaration;
import orc.ast.extended.declaration.type.DatatypeDeclaration;
import orc.ast.extended.declaration.type.TypeAliasDeclaration;
import orc.ast.extended.declaration.type.TypeDeclaration;
import orc.ast.extended.expression.Declare;
import orc.ast.extended.expression.Lambda;
import orc.ast.extended.expression.Pruning;
import orc.ast.extended.expression.Sequential;

import org.eclipse.imp.editor.ModelTreeNode;
import org.eclipse.imp.services.base.TreeModelBuilderBase;

/**
 * Builds an Outline view tree that is a subset of the Orc extended AST
 */
public class OrcTreeModelBuilder extends TreeModelBuilderBase {
	/* (non-Javadoc)
	 * @see org.eclipse.imp.services.base.TreeModelBuilderBase#visitTree(java.lang.Object)
	 */
	@Override
	public void visitTree(final Object root) {
		if (root == null) {
			return;
		}
		final ASTNode rootNode = (ASTNode) root;
		final OrcModelVisitor visitor = new OrcModelVisitor();

		rootNode.accept(visitor);
	}

	protected class OrcModelVisitor extends Walker {
		private boolean ignoreScope(final ASTNode node) {
			return node instanceof Declare
					|| node instanceof Lambda
					|| node instanceof Sequential
					|| node instanceof Pruning
					|| node.getSourceLocation() == null; // No location is confusing to the outline control
		}

		/* (non-Javadoc)
		 * @see orc.ast.extended.Walker#enterScope(orc.ast.extended.ASTNode)
		 */
		@Override
		public void enterScope(final ASTNode node) {
			if (!ignoreScope(node)) {
				super.enterScope(node);
				pushSubItem(node);
			}
		}

		/* (non-Javadoc)
		 * @see orc.ast.extended.Walker#leaveScope(orc.ast.extended.ASTNode)
		 */
		@Override
		public void leaveScope(final ASTNode node) {
			if (!ignoreScope(node)) {
				popSubItem();
				super.leaveScope(node);
			}
		}

		/* (non-Javadoc)
		 * @see orc.ast.extended.Walker#enter(orc.ast.extended.declaration.ClassDeclaration)
		 */
		@Override
		public boolean enter(final ClassDeclaration decl) {
			createSubItem(decl);
			return true;
		}

		/* (non-Javadoc)
		 * @see orc.ast.extended.Walker#enter(orc.ast.extended.declaration.type.DatatypeDeclaration)
		 */
		@Override
		public boolean enter(final DatatypeDeclaration decl) {
			createSubItem(decl);
			return true;
		}

		/* (non-Javadoc)
		 * @see orc.ast.extended.Walker#enter(orc.ast.extended.declaration.SiteDeclaration)
		 */
		@Override
		public boolean enter(final SiteDeclaration decl) {
			createSubItem(decl);
			return true;
		}

		/* (non-Javadoc)
		 * @see orc.ast.extended.Walker#enter(orc.ast.extended.declaration.type.TypeAliasDeclaration)
		 */
		@Override
		public boolean enter(final TypeAliasDeclaration decl) {
			createSubItem(decl);
			return true;
		}

		/* (non-Javadoc)
		 * @see orc.ast.extended.Walker#enter(orc.ast.extended.declaration.type.TypeDeclaration)
		 */
		@Override
		public boolean enter(final TypeDeclaration decl) {
			createSubItem(decl);
			return true;
		}

		/* (non-Javadoc)
		 * @see orc.ast.extended.Walker#enter(orc.ast.extended.declaration.ValDeclaration)
		 */
		@Override
		public boolean enter(final ValDeclaration decl) {
			createSubItem(decl);
			return true;
		}
	}

	/**
	 * Creates a child of the current node in the model tree.
	 * 
	 * @param n ASTNode to associate with this node in the model tree
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
	 * @param n ASTNode to associate with this node in the model tree
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
	 * @param n ASTNode to associate with this node in the model tree
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
	 * @param n ASTNode to associate with this node in the model tree
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
	 * @param n ASTNode to associate with this node in the model tree
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
	 * @param n ASTNode to associate with this node in the model tree
	 * @return ModeTreeNode created with previous current node as parent
	 * @see org.eclipse.imp.services.base.TreeModelBuilderBase#pushSubItem(java.lang.Object)
	 */
	@Override
	protected ModelTreeNode pushSubItem(final Object n) {
		return super.pushSubItem(n);
	}
}

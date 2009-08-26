//
// OrcSourcePositionLocator.java -- Java class OrcSourcePositionLocator
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Aug 9, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.parse;

import orc.ast.extended.ASTNode;
import orc.ast.extended.Walker;
import orc.error.Located;
import orc.error.SourceLocation;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.imp.editor.ModelTreeNode;
import org.eclipse.imp.parser.IParseController;
import org.eclipse.imp.parser.ISourcePositionLocator;

/**
 * Locates nodes in an AST, given offsets in a file 
 *
 * @author jthywiss
 */
public class OrcSourcePositionLocator implements ISourcePositionLocator {

	/**
	 * Constructs an object of class OrcSourcePositionLocator.
	 *
	 * @param parseController ParseController for these AST node instances
	 */
	public OrcSourcePositionLocator(final IParseController parseController) {
	}

	private final class NodeVisitor extends Walker {

		public ASTNode fNode;
		private final int fStartOffset;
		private final int fEndOffset;

		public NodeVisitor(final int startOffset, final int endOffset) {
			fStartOffset = startOffset;
			fEndOffset = endOffset;
		}

		@Override
		@SuppressWarnings("unused") // This is NOT an Unnecessary @SuppressWarnings("unused")	
		// enter(ASTNode) is called from Walker
		public boolean enter(final ASTNode element) {
			final int nodeStartOffset = getStartOffset(element);
			final int nodeEndOffset = getEndOffset(element);
//			System.out.println("OrcSourcePositionLocator.NodeVisitor.preVisit(ASTNode):  Examining " + element.getClass().getName() + " @ [" + nodeStartOffset + "->" + nodeEndOffset + ']');

			// If this node contains the span of interest then record it
			if (nodeStartOffset <= fStartOffset && nodeEndOffset >= fEndOffset) {
//				System.out.println("OrcSourcePositionLocator.NodeVisitor.preVisit(ASTNode) SELECTED for offsets [" + fStartOffset + ".." + fEndOffset + "]");
				fNode = element;
				return true; // to continue visiting here
			}
			return false; // to stop visiting here
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.ISourcePositionLocator#findNode(java.lang.Object, int)
	 */
	public Object findNode(final Object ast, final int offset) {
		return findNode(ast, offset, offset);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.ISourcePositionLocator#findNode(java.lang.Object, int, int)
	 */
	public Object findNode(final Object ast, final int startOffset, final int endOffset) {
		final NodeVisitor fVisitor = new NodeVisitor(startOffset, endOffset);
		
//		System.out.println("Looking for node spanning offsets " + startOffset + " => " + endOffset);

		if (startOffset < 0 || endOffset < 0) {
			fVisitor.fNode = null;
		} else {
			((ASTNode) ast).accept(fVisitor);
		}
		
//		if (fVisitor.fNode == null) {
//			System.out.println("Selected node:  null");
//		} else {
//			System.out.println("Selected node: " + fVisitor.fNode + " [" +
//					getStartOffset(fVisitor.fNode) + ".." + getEndOffset(fVisitor.fNode) + "]");
//		}
		return fVisitor.fNode;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.ISourcePositionLocator#getStartOffset(java.lang.Object)
	 */
	public int getStartOffset(final Object node) {
		if (node instanceof Located) {
			final Located n = (Located) node;
			if (n.getSourceLocation() == null) {
				return SourceLocation.UNKNOWN.offset;
			}
			return n.getSourceLocation().offset;
		} else if (node instanceof ModelTreeNode) {
			final ModelTreeNode treeNode = (ModelTreeNode) node;
			return getStartOffset(treeNode.getASTNode());
		}
		return SourceLocation.UNKNOWN.offset;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.ISourcePositionLocator#getEndOffset(java.lang.Object)
	 */
	public int getEndOffset(final Object node) {
		if (node instanceof Located) {
			final Located n = (Located) node;
			if (n.getSourceLocation() == null) {
				return SourceLocation.UNKNOWN.endOffset;
			}
			return n.getSourceLocation().endOffset;
		} else if (node instanceof ModelTreeNode) {
			final ModelTreeNode treeNode = (ModelTreeNode) node;
			return getStartOffset(treeNode.getASTNode());
		}
		return SourceLocation.UNKNOWN.endOffset;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.ISourcePositionLocator#getLength(java.lang.Object)
	 */
	public int getLength(final Object node) {
		return getEndOffset(node) - getStartOffset(node) + 1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.ISourcePositionLocator#getPath(java.lang.Object)
	 */
	public IPath getPath(final Object node) {
		final Located n = (Located) node;
		return Path.fromOSString(n.getSourceLocation().file.getPath());
	}
}

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

import edu.utexas.cs.orc.orceclipse.Activator;

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
		@SuppressWarnings("unused")
		// This is NOT an Unnecessary @SuppressWarnings("unused")	
		// enter(ASTNode) is called from Walker
		public boolean enter(final ASTNode element) {
			final int nodeStartOffset = getStartOffset(element);
			final int nodeEndOffset = getEndOffset(element);

			// If this node contains the span of interest then record it
			if (nodeStartOffset <= fStartOffset && nodeEndOffset >= fEndOffset) {
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

		if (startOffset < 0 || endOffset < 0) {
			fVisitor.fNode = null;
		} else {
			((ASTNode) ast).accept(fVisitor);
		}

		return fVisitor.fNode;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.ISourcePositionLocator#getStartOffset(java.lang.Object)
	 */
	public int getStartOffset(final Object node) {
		// node could be an AST node, a token, or a ModelTreeNode

		// Bizarrely, IMP requires, for unknown locations, both:
		//   offsets to be >= 0 (for annotations)
		//   offsets to be -1 (for text editor repositioning)
		// If we can distinguish these cases, we'll handle.  In the meantime, annotations win.

		if (node instanceof Located) {
			final Located n = (Located) node;
			if (n.getSourceLocation() == null || SourceLocation.UNKNOWN.equals(n)) {
				return 0;
			}
			return n.getSourceLocation().offset;
		} else if (node instanceof ModelTreeNode) {
			final Located n = (Located) ((ModelTreeNode) node).getASTNode();
			if (n.getSourceLocation() == null || SourceLocation.UNKNOWN.equals(n)) {
				return -1;
			}
			return n.getSourceLocation().offset;
		} else {
			final ClassCastException e = new ClassCastException(this.getClass().getName() + ".getStartOffset got an unrecognized node type"); //$NON-NLS-1$
			// Make sure this is logged -- Callers in IMP sometimes disregard exceptions
			Activator.log(e);
			throw e;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.ISourcePositionLocator#getLength(java.lang.Object)
	 */
	public int getLength(final Object node) {
		return getEndOffset(node) - getStartOffset(node) + 1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.ISourcePositionLocator#getEndOffset(java.lang.Object)
	 */
	public int getEndOffset(final Object node) {
		// node could be an AST node, a token, or a ModelTreeNode

		// IMP requires, for unknown locations, length to be -1
		// length = end - start + 1, thus for UNKNOWN, end must be
		// start - 2

		if (node instanceof Located) {
			final Located n = (Located) node;
			if (n.getSourceLocation() == null || SourceLocation.UNKNOWN.equals(n)) {
				return getStartOffset(node) - 2;
			}
			return n.getSourceLocation().endOffset;
		} else if (node instanceof ModelTreeNode) {
			final Located n = (Located) ((ModelTreeNode) node).getASTNode();
			if (n.getSourceLocation() == null || SourceLocation.UNKNOWN.equals(n)) {
				return getStartOffset(node) - 2;
			}
			return n.getSourceLocation().endOffset;
		} else {
			final ClassCastException e = new ClassCastException(this.getClass().getName() + ".getEndOffset got an unrecognized node type"); //$NON-NLS-1$
			// Make sure this is logged -- Callers in IMP sometimes disregard exceptions
			Activator.log(e);
			throw e;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.ISourcePositionLocator#getPath(java.lang.Object)
	 */
	public IPath getPath(final Object node) {
		try {
			final Located n = (Located) node;
			return Path.fromOSString(n.getSourceLocation().file.getPath());
		} catch (final ClassCastException e) {
			// Make sure this is logged -- Callers in IMP sometimes disregard exceptions
			Activator.log(e);
			throw e;
		}
	}
}

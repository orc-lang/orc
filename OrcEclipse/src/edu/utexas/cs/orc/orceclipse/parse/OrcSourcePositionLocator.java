//
// OrcSourcePositionLocator.java -- Java class OrcSourcePositionLocator
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Aug 9, 2009.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.parse;

import orc.ast.AST;
import orc.compile.parse.OrcPosition;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.imp.editor.ModelTreeNode;
import org.eclipse.imp.parser.IParseController;
import org.eclipse.imp.parser.ISourcePositionLocator;

import scala.collection.JavaConversions;
import scala.util.parsing.input.NoPosition$;
import scala.util.parsing.input.OffsetPosition;
import scala.util.parsing.input.Positional;
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
		/* Nothing to do */
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.ISourcePositionLocator#findNode(java.lang.Object, int)
	 */
	@Override
	public Object findNode(final Object ast, final int offset) {
		return findNode(ast, offset, offset);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.ISourcePositionLocator#findNode(java.lang.Object, int, int)
	 */
	@Override
	public Object findNode(final Object ast, final int startOffset, final int endOffset) {
		Object theWinner = null;
		if (startOffset < 0 || endOffset < 0) {
			return null;
		} else {
			for (final AST child : JavaConversions.asList(((AST) ast).subtrees())) {
				if (child.pos() instanceof OffsetPosition) {
					final int childOffset = ((OffsetPosition) child.pos()).offset();
					if (childOffset >= startOffset && childOffset <= endOffset && (theWinner == null || childOffset <= ((OffsetPosition) ((Positional) theWinner).pos()).offset())) {
						theWinner = findNode(child, startOffset, endOffset);
					}
				}
			}
		}

		return theWinner;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.ISourcePositionLocator#getStartOffset(java.lang.Object)
	 */
	@Override
	public int getStartOffset(final Object node) {
		// node could be an AST node, a token, or a ModelTreeNode

		// Bizarrely, IMP requires, for unknown locations, both:
		//   offsets to be >= 0 (for annotations)
		//   offsets to be -1 (for text editor repositioning)
		// If we can distinguish these cases, we'll handle.  In the meantime, annotations win.

		if (node instanceof Positional) {
			final Positional n = (Positional) node;
			if (n.pos() == null || n.pos() instanceof NoPosition$) {
				return 0;
			}
			return ((OffsetPosition) n.pos()).offset();
		} else if (node instanceof ModelTreeNode) {
			final Positional n = (Positional) ((ModelTreeNode) node).getASTNode();
			if (n.pos() == null || n.pos() instanceof NoPosition$) {
				return -1;
			}
			return ((OffsetPosition) n.pos()).offset();
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
	@Override
	public int getLength(final Object node) {
		return getEndOffset(node) - getStartOffset(node) + 1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.ISourcePositionLocator#getEndOffset(java.lang.Object)
	 */
	@Override
	public int getEndOffset(final Object node) {
		// node could be an AST node, a token, or a ModelTreeNode

		// IMP requires, for unknown locations, length to be -1
		// length = end - start + 1, thus for UNKNOWN, end must be
		// start - 2

		if (node instanceof OrcLexer.OrcToken) {
			final OrcLexer.OrcToken n = (OrcLexer.OrcToken) node;
			if (n.pos() == null || n.pos() instanceof NoPosition$) {
				return getStartOffset(node) - 2;
			}
			return getStartOffset(node) + n.text.length() - 1;
		} else if (node instanceof Positional) {
			final Positional n = (Positional) node;
			if (n.pos() == null || n.pos() instanceof NoPosition$) {
				return getStartOffset(node) - 2;
			}
			return getStartOffset(node);
		} else if (node instanceof ModelTreeNode) {
			final Positional n = (Positional) ((ModelTreeNode) node).getASTNode();
			if (n.pos() == null || n.pos() instanceof NoPosition$) {
				return getStartOffset(node) - 2;
			}
			return getStartOffset(node);
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
	@Override
	public IPath getPath(final Object node) {
		try {
			final Positional n = (Positional) node;
			return Path.fromOSString(((OrcPosition) n.pos()).filename());
		} catch (final ClassCastException e) {
			// Make sure this is logged -- Callers in IMP sometimes disregard exceptions
			Activator.log(e);
			throw e;
		}
	}
}

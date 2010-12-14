//
// OrcAnnotationHover.java -- Java class OrcAnnotationHover
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Aug 26, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.edit;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.imp.editor.AnnotationHoverBase;
import org.eclipse.imp.services.IAnnotationHover;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationHoverExtension;
import org.eclipse.jface.text.source.ILineRange;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.LineRange;

/**
 * Provides the text for a hover over an annotation in the editor's vertical ruler.
 *
 * @author jthywiss
 */
public class OrcAnnotationHover extends AnnotationHoverBase implements IAnnotationHover, IAnnotationHoverExtension {

	/* (non-Javadoc)
	 * @see org.eclipse.imp.editor.AnnotationHoverBase#getHoverInfo(org.eclipse.jface.text.source.ISourceViewer, int)
	 */
	@Override
	public String getHoverInfo(final ISourceViewer sourceViewer, final int lineNumber) {
		return getHoverInfo(sourceViewer, getHoverLineRange(sourceViewer, lineNumber), 1).toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.IAnnotationHoverExtension#getHoverControlCreator()
	 */
	@Override
	public IInformationControlCreator getHoverControlCreator() {
		return OrcHoverUtils.getHoverControlCreator();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.IAnnotationHoverExtension#canHandleMouseCursor()
	 */
	@Override
	public boolean canHandleMouseCursor() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.IAnnotationHoverExtension#getHoverInfo(org.eclipse.jface.text.source.ISourceViewer, org.eclipse.jface.text.source.ILineRange, int)
	 */
	@Override
	public Object getHoverInfo(final ISourceViewer sourceViewer, final ILineRange lineRange, final int visibleNumberOfLines) {
		final int lineNumber = lineRange.getStartLine();
		final List<Annotation> srcAnnotations = getSourceAnnotationsForLine(sourceViewer, lineNumber);

		final Set<String> annotationMessages = new HashSet<String>();
		for (final Iterator<Annotation> annoIter = srcAnnotations.iterator(); annoIter.hasNext();) {
			final Annotation annotation = annoIter.next();
			final String annoText = annotation.getText();
			if (annotation.getType().startsWith("org.eclipse.ui.workbench.texteditor.quickdiff") //$NON-NLS-1$
					|| annoText == null || annoText.trim().isEmpty() 
					|| annotationMessages.contains(annoText)) {
				annoIter.remove();
			} else {
				annotationMessages.add(annoText);
			}
		}
		return OrcHoverUtils.getHoverInfo(formatAnnotationList(srcAnnotations));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.IAnnotationHoverExtension#getHoverLineRange(org.eclipse.jface.text.source.ISourceViewer, int)
	 */
	@Override
	public ILineRange getHoverLineRange(final ISourceViewer viewer, final int lineNumber) {
		return new LineRange(lineNumber, 1);
	}

}

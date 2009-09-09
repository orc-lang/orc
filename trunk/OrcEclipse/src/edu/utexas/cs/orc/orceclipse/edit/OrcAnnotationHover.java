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
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;

/**
 * Provides the text for a hover over an annotation in the editor.
 * <p>
 * This subclass exists solely to fix IMP bugs.
 *
 * @author jthywiss
 */
public class OrcAnnotationHover extends AnnotationHoverBase implements IAnnotationHover {

	/* (non-Javadoc)
	 * @see org.eclipse.imp.editor.AnnotationHoverBase#getHoverInfo(org.eclipse.jface.text.source.ISourceViewer, int)
	 */
	@Override
	public String getHoverInfo(final ISourceViewer sourceViewer, final int lineNumber) {
		final List<Annotation> srcAnnotations = getSourceAnnotationsForLine(sourceViewer, lineNumber);

		final Set<String> annotationMessages = new HashSet<String>();
		for (final Iterator<Annotation> annoIter = srcAnnotations.iterator(); annoIter.hasNext();) {
			final Annotation annotation = annoIter.next();
			if (annotation.getType().startsWith("org.eclipse.ui.workbench.texteditor.quickdiff")) { //$NON-NLS-1$
				annoIter.remove();
			} else if (annotationMessages.contains(annotation.getText())) {
				annoIter.remove();
			} else {
				annotationMessages.add(annotation.getText());
			}
		}

		return formatAnnotationList(srcAnnotations);
	}

}

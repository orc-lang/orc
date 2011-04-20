//
// OrcTextHover.java -- Java class OrcTextHover
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Dec 13, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.edit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.imp.core.ErrorHandler;
import org.eclipse.imp.editor.HoverHelper;
import org.eclipse.imp.editor.UniversalEditor;
import org.eclipse.imp.language.ServiceFactory;
import org.eclipse.imp.parser.IModelListener;
import org.eclipse.imp.parser.IParseController;
import org.eclipse.imp.services.IHoverHelper;
import org.eclipse.imp.services.base.HoverHelperBase;
import org.eclipse.imp.utils.AnnotationUtils;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;

import edu.utexas.cs.orc.orceclipse.Activator;
import edu.utexas.cs.orc.orceclipse.Messages;

/**
 * Provides "tool tip" for the text in an Orc text editor.
 *
 * @author jthywiss
 */
public class OrcTextHover implements ITextHover, ITextHoverExtension, IModelListener {

	private IParseController controller;
	private IHoverHelper hoverHelper;

	/**
	 * Constructs an object of class OrcTextHover.
	 *
	 * @param editor The IMP editor for with to provide text tool tips
	 * @param previousTextHover The previously configured TextHover
	 */
	public OrcTextHover(final UniversalEditor editor, final IModelListener previousTextHover) {
		editor.fParserScheduler.removeModelListener(previousTextHover);
		hoverHelper = ServiceFactory.getInstance().getHoverHelper(editor.fLanguage);
		if (hoverHelper == null) {
			hoverHelper = new HoverHelper(editor.fLanguage);
		} else if (hoverHelper instanceof HoverHelperBase) {
			((HoverHelperBase) hoverHelper).setLanguage(editor.fLanguage);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.ITextHover#getHoverInfo(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public String getHoverInfo(final ITextViewer textViewer, final IRegion hoverRegion) {
		try {
			final int offset = hoverRegion.getOffset();
			String help = null;

			if (controller != null && hoverHelper != null) {
				help = hoverHelper.getHoverHelpAt(controller, (ISourceViewer) textViewer, offset);
			}
			if (help == null) {
				help = AnnotationUtils.formatAnnotationList(AnnotationUtils.getAnnotationsForOffset((ISourceViewer) textViewer, offset));
			}
			return OrcHoverUtils.getHoverInfo(help);
		} catch (final Throwable e) {
			ErrorHandler.reportError(Messages.OrcTextHover_HoverHelpThrew, e);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.ITextHover#getHoverRegion(org.eclipse.jface.text.ITextViewer, int)
	 */
	@Override
	public IRegion getHoverRegion(final ITextViewer textViewer, final int offset) {
		try {
			// IMP's TextHover shows all annotations on a line, so make the whole line the hover region
			final int line = textViewer.getDocument().getLineOfOffset(offset);
			return new Region(textViewer.getDocument().getLineOffset(line), textViewer.getDocument().getLineLength(line));
		} catch (final BadLocationException e) {
			Activator.log(e);
		}
		return new Region(offset, 0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.ITextHoverExtension#getHoverControlCreator()
	 */
	@Override
	public IInformationControlCreator getHoverControlCreator() {
		return OrcHoverUtils.getHoverControlCreator();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.IModelListener#getAnalysisRequired()
	 */
	@Override
	public AnalysisRequired getAnalysisRequired() {
		return AnalysisRequired.NAME_ANALYSIS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.IModelListener#update(org.eclipse.imp.parser.IParseController, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void update(final IParseController parseController, final IProgressMonitor monitor) {
		this.controller = parseController;
	}

}

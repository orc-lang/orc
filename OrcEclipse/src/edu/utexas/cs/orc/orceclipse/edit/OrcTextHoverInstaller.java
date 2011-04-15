//
// OrcTextHoverInstaller.java -- Java class OrcTextHoverInstaller
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Dec 13, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.edit;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.imp.editor.ServiceControllerManager;
import org.eclipse.imp.editor.StructuredSourceViewer;
import org.eclipse.imp.editor.UniversalEditor;
import org.eclipse.imp.language.ILanguageService;
import org.eclipse.imp.language.LanguageRegistry;
import org.eclipse.imp.parser.IModelListener;
import org.eclipse.imp.parser.IParseController;
import org.eclipse.imp.services.IEditorService;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import edu.utexas.cs.orc.orceclipse.Activator;

/**
 * This class exists so that we can replace IMP's TextHover.
 * Upon initialization (the setEditor call), we change the IMP
 * editor's textHover to a new OrcTextHover.
 *
 * @author jthywiss
 */
public class OrcTextHoverInstaller implements ILanguageService, IEditorService {

	/**
	 * Constructs an object of class OrcTextHoverInstaller.
	 *
	 */
	public OrcTextHoverInstaller() {
		/* Nothing to do */
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.IModelListener#getAnalysisRequired()
	 */
	@Override
	public AnalysisRequired getAnalysisRequired() {
		return AnalysisRequired.NONE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.IModelListener#update(org.eclipse.imp.parser.IParseController, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void update(final IParseController parseController, final IProgressMonitor monitor) {
		/* Nothing to do */
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.services.IEditorService#getName()
	 */
	@Override
	public String getName() {
		return getClass().getCanonicalName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.services.IEditorService#setEditor(org.eclipse.imp.editor.UniversalEditor)
	 */
	@Override
	public void setEditor(final UniversalEditor editor) {
		// Is this an Orc editor?
		if (editor.fLanguage == LanguageRegistry.findLanguage(Activator.getInstance().getLanguageID())) {
			try {
				// This gets ugly....
				final Field fServiceControllerManagerField = editor.getClass().getDeclaredField("fServiceControllerManager"); //$NON-NLS-1$
				fServiceControllerManagerField.setAccessible(true);
				final ServiceControllerManager fServiceControllerManager = (ServiceControllerManager) fServiceControllerManagerField.get(editor);

				final Field fHoverHelpControllerField = fServiceControllerManager.getClass().getDeclaredField("fHoverHelpController"); //$NON-NLS-1$
				fHoverHelpControllerField.setAccessible(true);
				final IModelListener fHoverHelpController = (IModelListener) fHoverHelpControllerField.get(fServiceControllerManager);

				final OrcTextHover newTextHover = new OrcTextHover(editor, fHoverHelpController);

				// IMP's ServiceControllerManager.fHoverHelpController's type is internal (private),
				// so we can't set it.
				//fHoverHelpControllerField.set(fServiceControllerManager, newTextHover);

				editor.fParserScheduler.addModelListener(newTextHover);

				final Method getSourceViewerMethod = AbstractTextEditor.class.getDeclaredMethod("getSourceViewer"); //$NON-NLS-1$
				getSourceViewerMethod.setAccessible(true);
				final StructuredSourceViewer sourceViewer = (StructuredSourceViewer) getSourceViewerMethod.invoke(editor);

				sourceViewer.setTextHover(newTextHover, IDocument.DEFAULT_CONTENT_TYPE);

			} catch (final SecurityException e) {
				Activator.logAndShow(e);
			} catch (final NoSuchFieldException e) {
				Activator.logAndShow(e);
			} catch (final IllegalArgumentException e) {
				Activator.logAndShow(e);
			} catch (final IllegalAccessException e) {
				Activator.logAndShow(e);
			} catch (final NoSuchMethodException e) {
				Activator.logAndShow(e);
			} catch (final InvocationTargetException e) {
				Activator.logAndShow(e);
			}
		}
	}

}

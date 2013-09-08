//
// OrcHoverUtils.java -- Java class OrcHoverUtils
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Dec 13, 2010.
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.edit;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.internal.text.html.BrowserInformationControl;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.statushandlers.StatusManager;

import edu.utexas.cs.orc.orceclipse.Activator;

/**
 * Utilities for the Orc editor "tool tip"s (hovers).
 *
 * @author jthywiss
 */
@SuppressWarnings("restriction")
public class OrcHoverUtils {

	private static final Pattern orcWikiLinkPattern = Pattern.compile("\\[\\[OrcWiki\\:([A-Za-z0-9_\\-\\#\\%]+)\\]\\]"); //$NON-NLS-1$
	private static final String orcWikiUri = "https://orc.csres.utexas.edu/wiki/Wiki.jsp?page="; //$NON-NLS-1$

	static IInformationControlCreator sharedInformationControlCreator;

	/*
	 * Derived from org.eclipse.jdt.ui/JavadocHoverStyleSheet.css,
	 * Revision 1.12 (24 Apr 2010), trunk rev as of 12 Dec 2010
	 */
	private static String getHoverHtmlHead() {
		return "<html><head><style type=\"text/css\">\n" + //$NON-NLS-1$
				"/* Font definitions */\n" + //$NON-NLS-1$
				"html        { font-family: \"" + JFaceResources.getDialogFont().getFontData()[0].getName() + "\"; font-size: " + JFaceResources.getDialogFont().getFontData()[0].getHeight() + "px; font-style: normal; font-weight: normal; }\n" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"body, h1, h2, h3, h4, h5, h6, p, table, td, caption, th, ul, ol, dl, li, dd, dt { font-size: 1em; }\n" + //$NON-NLS-1$
				"pre         { font-family: monospace; }\n" + //$NON-NLS-1$
				"\n" + //$NON-NLS-1$
				"/* Margins */\n" + //$NON-NLS-1$
				"body        { overflow: auto; margin-top: 0px; margin-bottom: 0.5em; margin-left: 0.3em; margin-right: 0px; }\n" + //$NON-NLS-1$
				"h1          { margin-top: 0.3em; margin-bottom: 0.04em; }\n" + //$NON-NLS-1$
				"h2          { margin-top: 2em; margin-bottom: 0.25em; }\n" + //$NON-NLS-1$
				"h3          { margin-top: 1.7em; margin-bottom: 0.25em; }\n" + //$NON-NLS-1$
				"h4          { margin-top: 2em; margin-bottom: 0.3em; }\n" + //$NON-NLS-1$
				"h5          { margin-top: 0px; margin-bottom: 0px; }\n" + //$NON-NLS-1$
				"p           { margin-top: 1em; margin-bottom: 1em; }\n" + //$NON-NLS-1$
				"pre         { margin-left: 0.6em; }\n" + //$NON-NLS-1$
				"ul	         { margin-top: 0px; margin-bottom: 1em; margin-left: 1em; padding-left: 1em; }\n" + //$NON-NLS-1$
				"li	         { margin-top: 0px; margin-bottom: 0px; }\n" + //$NON-NLS-1$
				"li p        { margin-top: 0px; margin-bottom: 0px; }\n" + //$NON-NLS-1$
				"ol	         { margin-top: 0px; margin-bottom: 1em; margin-left: 1em; padding-left: 1em; }\n" + //$NON-NLS-1$
				"dl	         { margin-top: 0px; margin-bottom: 1em; }\n" + //$NON-NLS-1$
				"dt	         { margin-top: 0px; margin-bottom: 0px; font-weight: bold; }\n" + //$NON-NLS-1$
				"dd	         { margin-top: 0px; margin-bottom: 0px; }\n" + //$NON-NLS-1$
				"\n" + //$NON-NLS-1$
				"/* Styles and colors */\n" + //$NON-NLS-1$
				"a:link	     { color: #0000FF; }\n" + //$NON-NLS-1$
				"a:hover     { color: #000080; }\n" + //$NON-NLS-1$
				"a:visited   { text-decoration: underline; }\n" + //$NON-NLS-1$
				"a.header:link    { text-decoration: none; color: #000000/*InfoText*/ }\n" + //$NON-NLS-1$
				"a.header:visited { text-decoration: none; color: #000000/*InfoText*/ }\n" + //$NON-NLS-1$
				"a.header:hover   { text-decoration: underline; color: #000080; }\n" + //$NON-NLS-1$
				"h4          { font-style: italic; }\n" + //$NON-NLS-1$
				"strong	     { font-weight: bold; }\n" + //$NON-NLS-1$
				"em	         { font-style: italic; }\n" + //$NON-NLS-1$
				"var	     { font-style: italic; }\n" + //$NON-NLS-1$
				"th	         { font-weight: bold; }\n" + //$NON-NLS-1$
				"</style></head>"; //$NON-NLS-1$
	}

	/**
	 * Get an InformationControlCreator that creates a  BrowserInformationControl for use
	 * in tool tips (hovers).
	 *
	 * @return a IInformationControlCreator singleton instance
	 */
	public static IInformationControlCreator getHoverControlCreator() {
		if (sharedInformationControlCreator == null) {
			sharedInformationControlCreator = new IInformationControlCreator() {
				@Override
				public IInformationControl createInformationControl(final Shell parent) {
					final String tooltipAffordanceString = EditorsUI.getTooltipAffordanceString();
					if (BrowserInformationControl.isAvailable(parent)) {
						final BrowserInformationControl iControl = new BrowserInformationControl(parent, JFaceResources.DIALOG_FONT, tooltipAffordanceString) {
							@Override
							public IInformationControlCreator getInformationPresenterControlCreator() {
								return sharedInformationControlCreator;
							}
						};
						iControl.addLocationListener(new LocationListener() {
							@Override
							public void changing(final LocationEvent event) {
								try {
									String loc = event.location;
									event.doit = false;
									if (loc.startsWith("about:")) { //$NON-NLS-1$
										/*
										 * Using the Browser.setText API triggers a location change to "about:blank".
										 * XXX: remove this code once https://bugs.eclipse.org/bugs/show_bug.cgi?id=130314 is fixed
										 */
										event.doit = "about:blank".equals(loc); //$NON-NLS-1$
										return;
									}
									URI uri;
									try {
										uri = new URI(loc);
									} catch (final URISyntaxException e) {
										// try it with a file (workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=237903 ):
										final File file = new File(loc);
										if (!file.exists()) {
											StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.getInstance().getID(), e.getLocalizedMessage(), e), StatusManager.SHOW);
											return;
										}
										uri = file.toURI();
										loc = uri.toASCIIString();
									}
									org.eclipse.ui.PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(loc));
								} catch (final PartInitException e) {
									Activator.log(e);
								} catch (final MalformedURLException e) {
									StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.getInstance().getID(), e.getLocalizedMessage(), e), StatusManager.SHOW);
								}
							}

							@Override
							public void changed(final LocationEvent event) {
								// Disregard
							}
						});
						return iControl;
					} else {
						return new DefaultInformationControl(parent, tooltipAffordanceString);
					}
				}
			};
		}
		return sharedInformationControlCreator;
	}

	/**
	 * Reformat IMP's hover text into HTML usable in a BrowserInformationControl
	 *
	 * @param impAnnotationText The HTML produced by IMP's hover text formatters
	 * @return HTML, appropriately styled for use by the control returned by the getHoverControlCreator().createInformationControl method
	 */
	public static String getHoverInfo(final String impAnnotationText) {
		if (impAnnotationText == null) {
			return null;
		}
		String s = impAnnotationText;
		s = orcWikiLinkPattern.matcher(s).replaceAll("<a href=\"" + orcWikiUri + "$1\">$0</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		s = s.replaceAll("<font size=-1>", "").replaceAll("</font>", ""); //Remove IMP's attempt at formatting //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		s = getHoverHtmlHead() + s.substring(6);
		return s;
	}

}

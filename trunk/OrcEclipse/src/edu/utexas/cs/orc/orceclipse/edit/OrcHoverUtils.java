//
// OrcHoverUtils.java -- Java class OrcHoverUtils
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

	private static final Pattern orcWikiLinkPattern = Pattern.compile("\\[\\[OrcWiki\\:([A-Za-z0-9_\\-\\#\\%]+)\\]\\]");
	private static final String orcWikiUri = "http://orc.csres.utexas.edu/wiki/Wiki.jsp?page="; //$NON-NLS-1$
	/*
	 * Derived from org.eclipse.jdt.ui/JavadocHoverStyleSheet.css,
	 * Revision 1.12 (24 Apr 2010), trunk rev as of 12 Dec 2010 
	 */
	private static final String hoverHtmlHead = "<html><head><style type=\"text/css\">\n" +
	"/* Font definitions */\n" +
	"html        { font-family: \""+JFaceResources.getDialogFont().getFontData()[0].getName()+"\"; font-size: "+JFaceResources.getDialogFont().getFontData()[0].getHeight()+"px; font-style: normal; font-weight: normal; }\n" +
	"body, h1, h2, h3, h4, h5, h6, p, table, td, caption, th, ul, ol, dl, li, dd, dt { font-size: 1em; }\n" +
	"pre         { font-family: monospace; }\n" +
	"\n" +
	"/* Margins */\n" +
	"body        { overflow: auto; margin-top: 0px; margin-bottom: 0.5em; margin-left: 0.3em; margin-right: 0px; }\n" +
	"h1          { margin-top: 0.3em; margin-bottom: 0.04em; }\n" +
	"h2          { margin-top: 2em; margin-bottom: 0.25em; }\n" +
	"h3          { margin-top: 1.7em; margin-bottom: 0.25em; }\n" +
	"h4          { margin-top: 2em; margin-bottom: 0.3em; }\n" +
	"h5          { margin-top: 0px; margin-bottom: 0px; }\n" +
	"p           { margin-top: 1em; margin-bottom: 1em; }\n" +
	"pre         { margin-left: 0.6em; }\n" +
	"ul	         { margin-top: 0px; margin-bottom: 1em; margin-left: 1em; padding-left: 1em; }\n" +
	"li	         { margin-top: 0px; margin-bottom: 0px; }\n" +
	"li p        { margin-top: 0px; margin-bottom: 0px; }\n" +
	"ol	         { margin-top: 0px; margin-bottom: 1em; margin-left: 1em; padding-left: 1em; }\n" +
	"dl	         { margin-top: 0px; margin-bottom: 1em; }\n" +
	"dt	         { margin-top: 0px; margin-bottom: 0px; font-weight: bold; }\n" +
	"dd	         { margin-top: 0px; margin-bottom: 0px; }\n" +
	"\n" +
	"/* Styles and colors */\n" +
	"a:link	     { color: #0000FF; }\n" +
	"a:hover     { color: #000080; }\n" +
	"a:visited   { text-decoration: underline; }\n" +
	"a.header:link    { text-decoration: none; color: #000000/*InfoText*/ }\n" +
	"a.header:visited { text-decoration: none; color: #000000/*InfoText*/ }\n" +
	"a.header:hover   { text-decoration: underline; color: #000080; }\n" +
	"h4          { font-style: italic; }\n" +
	"strong	     { font-weight: bold; }\n" +
	"em	         { font-style: italic; }\n" +
	"var	     { font-style: italic; }\n" +
	"th	         { font-weight: bold; }\n" +
	"</style></head>";
	static IInformationControlCreator sharedInformationControlCreator;

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
						final BrowserInformationControl iControl = new BrowserInformationControl(parent, JFaceResources.DIALOG_FONT, tooltipAffordanceString)  {
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
		s = orcWikiLinkPattern.matcher(s).replaceAll("<a href=\"" + orcWikiUri + "$1\">$0</a>");
		s = s.replaceAll("<font size=-1>", "").replaceAll("</font>", ""); //Remove IMP's attempt at formatting
		s = hoverHtmlHead + s.substring(6);
		return s;
	}

}

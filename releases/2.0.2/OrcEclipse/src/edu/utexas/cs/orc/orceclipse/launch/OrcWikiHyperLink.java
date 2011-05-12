//
// OrcWikiHyperLink.java -- Java class OrcWikiHyperLink
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Aug 12, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.launch;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.statushandlers.StatusManager;

import edu.utexas.cs.orc.orceclipse.Activator;

/**
 * A link in a TextConsole that, when clicked, opens the external Web
 * browser to the given page on the Orc Wiki.
 *
 * @author jthywiss
 */
public class OrcWikiHyperLink implements IHyperlink {

	private static final String orcWikiUri = "http://orc.csres.utexas.edu/wiki/Wiki.jsp?page="; //$NON-NLS-1$
	protected String pageName;

	/**
	 * Constructs an object of class OrcWikiHyperLink.
	 *
	 * @param console TextConsole on which the link is displayed
	 * @param matchedName String that was matched as an Orc Wiki page name
	 */
	public OrcWikiHyperLink(final TextConsole console, final String matchedName) {
		super();
		pageName = matchedName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IHyperlink#linkActivated()
	 */
	@Override
	public void linkActivated() {
		try {
			org.eclipse.ui.PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(orcWikiUri + pageName));
		} catch (final PartInitException e) {
			Activator.logAndShow(e);
		} catch (final MalformedURLException e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.getInstance().getID(), e.getLocalizedMessage(), e), StatusManager.SHOW);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IHyperlink#linkEntered()
	 */
	@Override
	public void linkEntered() {
		/* Nothing to do */
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IHyperlink#linkExited()
	 */
	@Override
	public void linkExited() {
		/* Nothing to do */
	}

}

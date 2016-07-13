//
// HttpHyperLink.java -- Java class HttpHyperLink
// Project OrcEclipse
//
// Created by jthywiss on Aug 13, 2010.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
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

import edu.utexas.cs.orc.orceclipse.OrcPlugin;

/**
 * A link in a TextConsole that, when clicked, opens as a URI in the external
 * Web browser.
 *
 * @author jthywiss
 */
public class HttpHyperLink implements IHyperlink {

    protected String uriText;

    /**
     * Constructs an object of class HttpHyperLink.
     *
     * @param console TextConsole on which the link is displayed
     * @param matchedText String that was matched as a URI
     */
    public HttpHyperLink(final TextConsole console, final String matchedText) {
        super();
        uriText = matchedText;
    }

    @Override
    public void linkActivated() {
        try {
            org.eclipse.ui.PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(uriText));
        } catch (final PartInitException e) {
            OrcPlugin.logAndShow(e);
        } catch (final MalformedURLException e) {
            StatusManager.getManager().handle(new Status(IStatus.ERROR, OrcPlugin.getId(), e.getLocalizedMessage(), e), StatusManager.SHOW);
        }
    }

    @Override
    public void linkEntered() {
        /* Nothing to do */
    }

    @Override
    public void linkExited() {
        /* Nothing to do */
    }

}

//
// HttpPatternMatchListener.java -- Java class HttpPatternMatchListener
// Project OrcEclipse
//
// Created by jthywiss on Aug 13, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.launch;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IPatternMatchListenerDelegate;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

/**
 * Receives messages when a TextConsole's content matches the regex supplied in
 * plugin.xml, and creates a hyperlink in the console.
 *
 * @author jthywiss
 */
public class HttpPatternMatchListener implements IPatternMatchListenerDelegate {

    protected TextConsole observedConsole;

    /**
     * Constructs an object of class HttpPatternMatchListener.
     */
    public HttpPatternMatchListener() {
        super();
    }

    @Override
    public void connect(final TextConsole console) {
        observedConsole = console;
    }

    @Override
    public void disconnect() {
        observedConsole = null;
    }

    @Override
    public void matchFound(final PatternMatchEvent event) {
        try {
            final int offset = event.getOffset();
            final int length = event.getLength();
            final String uriText = observedConsole.getDocument().get(offset, length);
            final IHyperlink link = new HttpHyperLink(observedConsole, uriText);
            observedConsole.addHyperlink(link, offset, length);
        } catch (final BadLocationException e) {
            /* Discard */
        }
    }

}

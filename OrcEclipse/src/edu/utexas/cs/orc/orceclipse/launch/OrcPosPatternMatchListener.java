//
// OrcPosPatternMatchListener.java -- Java class OrcPosPatternMatchListener
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.ui.console.FileLink;
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
public class OrcPosPatternMatchListener implements IPatternMatchListenerDelegate {

    protected TextConsole observedConsole;

    /**
     * Constructs an object of class OrcPosPatternMatchListener.
     */
    public OrcPosPatternMatchListener() {
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
            final String matchedText = observedConsole.getDocument().get(offset, length - 1);
            final int lineColSep = matchedText.lastIndexOf(':');
            if (lineColSep < 3) {
                return;
            }
            final int nameLineSep = matchedText.lastIndexOf(':', lineColSep - 1);
            if (nameLineSep < 1) {
                return;
            }
            final IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(matchedText.substring(0, nameLineSep)));
            if (file == null) {
                return;
            }
            final int lineNum = Integer.parseInt(matchedText.substring(nameLineSep + 1, lineColSep));
            if (lineNum < 1) {
                return;
            }
            final int colNum = Integer.parseInt(matchedText.substring(lineColSep + 1));
            if (colNum < 1) {
                return;
            }
            final IHyperlink link = new FileLink(file, null, -1, -1, lineNum);
            observedConsole.addHyperlink(link, offset, length - 1);
        } catch (final BadLocationException e) {
            /* Discard */
        } catch (final NumberFormatException e) {
            /* Discard */
        }
    }

}

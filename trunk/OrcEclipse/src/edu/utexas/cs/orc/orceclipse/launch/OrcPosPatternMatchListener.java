//
// OrcPosPatternMatchListener.java -- Java class OrcPosPatternMatchListener
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Aug 13, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
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
 * Receives messages when a TextConsole's content matches the regex
 * supplied in plugin.xml, and creates a hyperlink in the console.
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

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IPatternMatchListenerDelegate#connect(org.eclipse.ui.console.TextConsole)
	 */
	@Override
	public void connect(TextConsole console) {
		observedConsole = console;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IPatternMatchListenerDelegate#disconnect()
	 */
	@Override
	public void disconnect() {
		observedConsole = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IPatternMatchListenerDelegate#matchFound(org.eclipse.ui.console.PatternMatchEvent)
	 */
	@Override
	public void matchFound(PatternMatchEvent event) {
		try {
			int offset = event.getOffset();
			int length = event.getLength();
			String matchedText = observedConsole.getDocument().get(offset, length-1); // -1 too drop final ": "
			int lineColSep = matchedText.lastIndexOf(':');
			if (lineColSep < 3) return;
			int nameLineSep = matchedText.lastIndexOf(':', lineColSep-1);
			if (nameLineSep < 1) return;
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(matchedText.substring(0, nameLineSep)));
			if (file == null) return;
			int lineNum = Integer.parseInt(matchedText.substring(nameLineSep+1, lineColSep));
			if (lineNum < 1) return;
			int colNum = Integer.parseInt(matchedText.substring(lineColSep+1));
			if (colNum < 1) return;
			IHyperlink link = new FileLink(file, null, -1, -1, lineNum);
			observedConsole.addHyperlink(link, offset, length-1);
		} catch (BadLocationException e) {
		} catch (NumberFormatException e) {
		}
	}

}

//
// WindowUtilities.java -- Java class WindowUtilities
// Project OrcSites
//
// $Id$
//
// Copyright (c) 2008 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.simanim;

import java.awt.Color;
import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.UIManager;

/** A few utilities that simplify testing of windows in Swing.
 *  1998 Marty Hall, http://www.apl.jhu.edu/~hall/java/
 */

public class WindowUtilities {
	/** Tell system to use native look and feel, as in previous
	 *  releases. Metal (Java) LAF is the default otherwise.
	 */

	public static void setNativeLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (final Exception e) {
			System.out.println("Error setting native LAF: " + e);
		}
	}

	/** A simplified way to see a JPanel or other Container.
	 *  Pops up a JFrame with specified Container as the content pane.
	 */

	public static JFrame openInJFrame(final Container content, final int width, final int height, final String title, final Color bgColor) {
		final JFrame frame = new JFrame(title);
		frame.setBackground(bgColor);
		content.setBackground(bgColor);
		frame.setSize(width, height);
		frame.setContentPane(content);
		frame.setVisible(true);
		return frame;
	}

	/** Uses Color.white as the background color. */

	public static JFrame openInJFrame(final Container content, final int width, final int height, final String title) {
		return openInJFrame(content, width, height, title, Color.white);
	}

	/** Uses Color.white as the background color, and the
	 *  name of the Container's class as the JFrame title.
	 */

	public static JFrame openInJFrame(final Container content, final int width, final int height) {
		return openInJFrame(content, width, height, content.getClass().getName(), Color.white);
	}
}

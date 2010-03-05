//
// ButtonPanel.java -- Java class ButtonPanel
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.gui;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class ButtonPanel extends JPanel {
	private boolean hasButtons = false;

	public ButtonPanel() {
		setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
	}

	@Override
	public Component add(final Component button) {
		if (hasButtons) {
			super.add(Box.createHorizontalStrut(5));
		} else {
			super.add(Box.createHorizontalGlue());
			hasButtons = true;
		}
		return super.add(button);
	}
}

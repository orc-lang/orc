/**
 * Copyright (c) 2009, The University of Texas at Austin ("U. T. Austin")
 * All rights reserved.
 *
 * <p>You may distribute this file under the terms of the OSI Simplified BSD License,
 * as defined in the LICENSE file found in the project's top-level directory.
 */
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
	public Component add(Component button) {
		if (hasButtons) {
			super.add(Box.createHorizontalStrut(5));
		} else {
			super.add(Box.createHorizontalGlue());
			hasButtons = true;
		}
		return super.add(button);
	}
}
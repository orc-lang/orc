//
// OneColumnPanel.java -- Java class OneColumnPanel
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

/**
 * Panel with a simple one-column layout.
 * Useful for forms which are just a list of fields.
 * @author quark
 */
public class OneColumnPanel extends JPanel {
	private final GridBagConstraints c = new GridBagConstraints();
	{
		c.gridheight = 1;
		c.gridy = GridBagConstraints.RELATIVE;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
	}

	public OneColumnPanel() {
		super(new GridBagLayout());
	}

	@Override
	public Component add(final Component row) {
		super.add(row, c);
		return row;
	}
}

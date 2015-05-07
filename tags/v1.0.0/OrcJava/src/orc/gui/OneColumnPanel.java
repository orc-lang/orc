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
	public Component add(Component row) {
		super.add(row, c);
		return row;
	}
}
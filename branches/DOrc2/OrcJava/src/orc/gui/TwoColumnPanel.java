package orc.gui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;

/**
 * Panel with a simple two-column layout.
 * Useful for forms which are just a list of fields.
 * @author quark
 */
public class TwoColumnPanel extends JPanel {
	private final GridBagConstraints c = new GridBagConstraints();
	{
		c.gridheight = 1;
		c.gridy = GridBagConstraints.RELATIVE;
		c.fill = GridBagConstraints.HORIZONTAL;
	}
	private final Insets insetRight = new Insets(0, 0, 0, 5);
	private final Insets insetNone = new Insets(0, 0, 0, 0);
	
	public TwoColumnPanel() {
		super(new GridBagLayout());
	}
	
	/**
	 * Rows with one component span both columns.
	 */
	@Override
	public Component add(Component row) {
		c.gridx = 0;
		c.gridwidth = 2; 
		super.add(row, c);
		return row;
	}

	/**
	 * Rows with two components are split into two columns,
	 * with 5px gap.
	 */
	protected void addRow(Component label, Component field) {
		c.gridwidth = 1; 
		c.gridx = 0;
		c.insets = insetRight;
		add(label, c);
		c.insets = insetNone;
		c.gridx = 1;
		add(field, c);
	}
}
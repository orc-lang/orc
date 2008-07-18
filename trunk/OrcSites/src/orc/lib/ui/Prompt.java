package orc.lib.ui;

import javax.swing.JOptionPane;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.ThreadedPartialSite;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;

/**
 * A prompt dialog. Publishes the user's response. If the
 * user hits Cancel, publishes nothing.
 */
public class Prompt extends ThreadedPartialSite {
	public Value evaluate(Args args) throws TokenException {
		String message = args.stringArg(0);
		String response = JOptionPane.showInputDialog(message);
		if (response == null) return null;
		else return new Constant(response);
	}
}

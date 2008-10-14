package orc.lib.util;

import javax.swing.JOptionPane;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.ThreadedPartialSite;

/**
 * A prompt dialog. Publishes the user's response. If the
 * user hits Cancel, publishes nothing.
 */
public class Prompt extends ThreadedPartialSite {
	public Object evaluate(Args args) throws TokenException {
		String message = args.stringArg(0);
		return JOptionPane.showInputDialog(message);
	}
}

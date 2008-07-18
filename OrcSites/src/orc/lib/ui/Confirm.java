package orc.lib.ui;

import javax.swing.JOptionPane;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.ThreadedPartialSite;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;

/**
 * A Yes/No/Cancel confirmation dialog. "Yes" = true, "No" = false,
 * and "Cancel" = null.
 */
public class Confirm extends ThreadedPartialSite {
	public Value evaluate(Args args) throws TokenException {
		String message = args.stringArg(0);
		int chosen = JOptionPane.showConfirmDialog(null, message);
		switch (chosen) {
		case 0: // YES
			return new Constant(true);
		case 1: // NO
			return new Constant(false);
		}
		// CANCEL
		return null;
	}
}

package orc.lib.ui;

import javax.swing.JOptionPane;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.ThreadedPartialSite;

/**
 * A Yes/No/Cancel confirmation dialog. "Yes" = true, "No" = false,
 * and "Cancel" = null.
 */
public class Confirm extends ThreadedPartialSite {
	public Object evaluate(Args args) throws TokenException {
		String message = args.stringArg(0);
		int chosen = JOptionPane.showConfirmDialog(null, message);
		switch (chosen) {
		case 0: // YES
			return true;
		case 1: // NO
			return false;
		}
		// CANCEL
		return null;
	}
}

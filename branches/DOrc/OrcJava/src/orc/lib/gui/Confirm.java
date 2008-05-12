package orc.lib.gui;

import javax.swing.JOptionPane;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PassedByValueSite;
import orc.runtime.sites.ThreadedPartialSite;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;

/**
 * A factory for Confirm dialogs.
 */
public class Confirm extends EvalSite implements PassedByValueSite {
	public Value evaluate(Args args) throws OrcRuntimeTypeException {
		// get arguments
		String message = args.stringArg(0);
		return new orc.runtime.values.Site(new ConfirmInstance(message));
	}
	
	/**
	 * Confirm instances are not passed by value so that one machine may
	 * pop up a dialog on another.
	 */
	public static class ConfirmInstance extends ThreadedPartialSite {
		private String message;
		public ConfirmInstance(String message) {
			this.message = message;
		}
		@Override
		public Value evaluate(Args args) throws OrcRuntimeTypeException {
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
}

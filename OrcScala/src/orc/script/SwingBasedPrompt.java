//
// SwingBasedPrompt.java -- Java class SwingBasedPrompt
// Project OrcScala
//
// $Id: SwingBasedPrompt.java 2378 2011-01-25 16:41:09Z dkitchin $
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.script;

import java.awt.Dialog.ModalityType;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * A prompt dialog. Publishes the user's response. If the user hits Cancel,
 * publishes nothing.
 */
public class SwingBasedPrompt {
	protected static final String promptIconName = "orcPromptIcon.png";
	protected static final Icon promptIcon = new ImageIcon(SwingBasedPrompt.class.getResource(promptIconName));

	public static String runPromptDialog(final String title, final String message) throws InterruptedException {
		final PromptWindowController pwc = new PromptWindowController(title, message, promptIcon);
		// Runs pwc.run on the AWT/Swing event dispatch thread
		SwingUtilities.invokeLater(pwc);
		// Blocks until dialog done
		return pwc.getResult();
	}
}

class PromptWindowController implements Runnable {
		private final String title;
		private final String message;
		private String result;
		private JOptionPane pane;
		private JDialog dialog;
		private boolean done;
		private final Icon icon;

		protected PromptWindowController(final String dialogTitle, final String dialogMessage, final Icon dialogIcon) {
			super();
			this.title = dialogTitle;
			this.message = dialogMessage;
			this.icon = dialogIcon;
		}

		synchronized protected String getResult() throws InterruptedException {
			while (!done) {
				wait();
			}
			return result;
		}

		protected void startDialog() {
			pane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, icon, null, null);

			pane.setWantsInput(true);
			pane.setSelectionValues(null);
			pane.setInitialSelectionValue(null);
			pane.setComponentOrientation(JOptionPane.getRootFrame().getComponentOrientation());

			dialog = pane.createDialog(title);
			dialog.setModalityType(ModalityType.MODELESS);
			dialog.setLocationByPlatform(true);

			pane.selectInitialValue();
			dialog.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(final ComponentEvent e) {
					endDialog();
				}
			});

			dialog.setVisible(true);
		}

		protected void endDialog() {
			dialog.dispose();

			final Object value = pane.getInputValue();

			if (value == JOptionPane.UNINITIALIZED_VALUE) {
				result = null;
			} else {
				result = (String) value;
			}
			done = true;
			synchronized (this) {
				notifyAll();
			}
		}

		@Override
		public void run() {
			startDialog();
		}
}

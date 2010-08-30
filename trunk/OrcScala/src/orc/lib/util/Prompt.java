//
// Prompt.java -- Java class Prompt
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.util;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JOptionPane;

import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.ThreadedPartialSite;
import orc.values.sites.compatibility.type.Type;
import orc.values.sites.compatibility.type.structured.ArrowType;

/**
 * A prompt dialog. Publishes the user's response. If the
 * user hits Cancel, publishes nothing.
 */
public class Prompt extends ThreadedPartialSite {
	@Override
	public Object evaluate(final Args args) throws TokenException {
		final String message = args.stringArg(0);
		return JOptionPane.showInputDialog(new UnsharedOwnerFrame(), message);
	}


	@Override
	public Type type() {
		return new ArrowType(Type.STRING, Type.STRING);
	}

	static protected class UnsharedOwnerFrame extends Frame implements WindowListener {
		/**
		 * Constructs an object of class UnsharedOwnerFrame.
		 *
		 * @throws HeadlessException
		 */
		public UnsharedOwnerFrame() throws HeadlessException {
			super();
		}

		private static final long serialVersionUID = -3357239973427463753L;

		@Override
		public void addNotify() {
			super.addNotify();
			installListeners();
		}

		/**
		 * Install window listeners on owned windows to watch for displayability
		 * changes
		 */
		void installListeners() {
			for (final Window window : getOwnedWindows()) {
				if (window != null) {
					window.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
					window.removeWindowListener(this);
					window.addWindowListener(this);
				}
			}
		}

		/**
		 * Watches for displayability changes and disposes shared instance if
		 * there are no displayable children left.
		 */
		@Override
		public void windowClosed(WindowEvent e) {
			synchronized (getTreeLock()) {
				for (final Window window : getOwnedWindows()) {
					if (window != null) {
						window.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
						if (window.isDisplayable()) {
							return;
						}
						window.removeWindowListener(this);
					}
				}
				dispose();
			}
		}

		@Override
		public void windowOpened(WindowEvent e) {
		}

		@Override
		public void windowClosing(WindowEvent e) {
		}

		@Override
		public void windowIconified(WindowEvent e) {
		}

		@Override
		public void windowDeiconified(WindowEvent e) {
		}

		@Override
		public void windowActivated(WindowEvent e) {
		}

		@Override
		public void windowDeactivated(WindowEvent e) {
		}

		@SuppressWarnings("deprecation") //We're just diabling show(), so it's OK that it's deprecated
		@Override
		public void show() {
			// This frame can never be shown
		}

		@Override
		public void dispose() {
			try {
				getToolkit().getSystemEventQueue();
				super.dispose();
			} catch (Exception e) {
				// untrusted code not allowed to dispose
			}
		}
	}

}

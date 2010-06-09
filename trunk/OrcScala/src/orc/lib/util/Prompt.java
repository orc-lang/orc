//
// Prompt.java -- Java class Prompt
// Project OrcJava
//
// $Id: Prompt.java 1502 2010-02-03 06:25:53Z jthywissen $
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.util;

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
		return JOptionPane.showInputDialog(message);
	}

	@Override
	public Type type() {
		return new ArrowType(Type.STRING, Type.STRING);
	}
}

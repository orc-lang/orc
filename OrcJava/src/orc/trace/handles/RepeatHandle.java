//
// RepeatHandle.java -- Java class RepeatHandle
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2008 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.trace.handles;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * @see Handle
 * @author quark
 */
public class RepeatHandle<E> extends Handle<E> {
	public RepeatHandle() {
		super();
	}

	public RepeatHandle(final E value) {
		super(value);
	}

	public void readExternal(final ObjectInput _in) throws IOException, ClassNotFoundException {
		final HandleInputStream in = (HandleInputStream) _in;
		final int id = in.readInt();
		value = (E) in.getHandled(id);
	}

	public void writeExternal(final ObjectOutput _out) throws IOException {
		final HandleOutputStream out = (HandleOutputStream) _out;
		out.writeInt(out.getHandle(value));
	}
}

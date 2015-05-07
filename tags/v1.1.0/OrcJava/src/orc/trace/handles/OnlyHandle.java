//
// OnlyHandle.java -- Java class OnlyHandle
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
public class OnlyHandle<E> extends Handle<E> {
	public OnlyHandle() {
		super();
	}

	public OnlyHandle(final E value) {
		super(value);
	}

	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		value = (E) in.readObject();
	}

	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeObject(value);
	}
}

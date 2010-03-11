//
// TraceableValue.java -- Java interface TraceableValue
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

package orc.trace.values;

/**
 * Mark a runtime object which may be traced (serialized in a trace file). If
 * the object is mutable it must also override {@link Object#equals(Object)} and
 * {@link Object#hashCode()} to allow traced representations to be cached
 * correctly.
 * 
 * @see Marshaller#marshal(Object)
 * @author quark
 */
public interface TraceableValue {
	public Value marshal(Marshaller tracer);
}

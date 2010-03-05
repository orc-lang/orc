//
// Value.java -- Java interface Value
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

import java.io.Serializable;

import orc.trace.Term;

/**
 * Supertype for traced value representations.
 * @author quark
 */
public interface Value extends Serializable, Term {
}

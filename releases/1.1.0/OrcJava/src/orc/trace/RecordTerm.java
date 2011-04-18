//
// RecordTerm.java -- Java interface RecordTerm
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

package orc.trace;

/**
 * A term with properties (like an object).
 * FIXME: need to figure out how to pattern match this.
 * @author quark
 */
public interface RecordTerm extends Term {
	public Term getProperty(String key);
}
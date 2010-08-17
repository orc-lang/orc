//
// Type.java -- Java class Type
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

package orc.values.sites.compatibility.type;

/**
 * Type singletons
 * 
 * @author dkitchin
 */
public abstract class Type {

	/* Create singleton representatives for some common types */
	public static final Type TOP = null;//new Top();
	public static final Type BOT = null;//new Bot();
	public static final Type NUMBER = null;//new NumberType();
	public static final Type STRING = null;//new StringType();
	public static final Type BOOLEAN = null;//new BooleanType();
	public static final Type INTEGER = null;//new IntegerType();
	public static final Type LET = null;//new LetType();
	public static final Type SIGNAL = null;//new SignalType();

}

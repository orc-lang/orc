//
// Type.java -- Java class Type
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.extended.type;

/**
 * Abstract superclass of syntactic types in the extended AST.
 * 
 * Syntactic types occur in all of the AST forms. The typechecker
 * converts them to a different form (subclasses of orc.type.Type)
 * for its own internal use.
 * 
 * Syntactic types do not have methods like meet, join, and subtype; only their
 * typechecker counterparts do. Thus, syntactic types permit only the simplest
 * analyses; more complex analyses must wait until the syntactic type is
 * converted within the typechecker.
 * 
 * All syntactic types can be written explicitly in a program, whereas
 * many of the typechecker's internal types are not representable in programs.
 * 
 * @author dkitchin
 */
public abstract class Type {

	/* Create singleton representatives for some common types */
	public static final Type TOP = new Top();
	public static final Type BOT = new Bot();

	/** 
	 * Convert this extended AST type into a simple AST type.
	 */
	public abstract orc.ast.simple.type.Type simplify();

}

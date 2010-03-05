//
// InferenceContinuation.java -- Java class InferenceContinuation
// Project OrcJava
//
// $Id$
//
// Created by dkitchin on Feb 5, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.type.inference;

import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.structured.ArrowType;

/**
 * 
 * A continuation to invoke type argument inference during
 * type checking. Whenever the typechecker enters the context
 * of a Call which has no type arguments, an inference
 * continuation is added to the context which will infer those
 * arguments if they are needed.
 *
 * @author dkitchin
 */
public abstract class InferenceContinuation {
	public abstract Type inferFrom(ArrowType arrowType) throws TypeException;
}

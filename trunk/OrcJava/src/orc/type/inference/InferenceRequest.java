//
// InferenceRequest.java -- Java class InferenceRequest
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

package orc.type.inference;

import orc.error.compiletime.typing.TypeException;
import orc.type.structured.ArrowType;

/**
 * A special exception raised and caught within the typechecker to request
 * the inference of missing type parameters on a call.
 * 
 * @author dkitchin
 */
public class InferenceRequest extends TypeException {

	public ArrowType requestedType;
	public boolean replay = false; // rerun the checker on this expression after inference?

	public InferenceRequest(final ArrowType requestedType) {
		super("Type parameters missing");
		this.requestedType = requestedType;
	}

	/**
	 * @return this InferenceRequest
	 */
	public Exception addReplay() {
		replay = true;
		return this;
	}
}

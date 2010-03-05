//
// InferredType.java -- Java class InferredType
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil.type;

import orc.error.compiletime.typing.TypeException;
import orc.type.TypingContext;

/**
 * A syntactic container for a type inferred during typechecking.
 * Such types are temporary, and cannot be marshalled to XML;
 * they marshal to null, since they were originally null before
 * being inferred.
 * 
 * @author dkitchin
 *
 */
public class InferredType extends Type {

	public orc.type.Type inferredType;

	public InferredType(final orc.type.Type inferredType) {
		this.inferredType = inferredType;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (inferredType == null ? 0 : inferredType.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final InferredType other = (InferredType) obj;
		if (inferredType == null) {
			if (other.inferredType != null) {
				return false;
			}
		} else if (!inferredType.equals(other.inferredType)) {
			return false;
		}
		return true;
	}

	@Override
	public orc.type.Type transform(final TypingContext ctx) throws TypeException {
		return inferredType;
	}

	@Override
	public orc.ast.xml.type.Type marshal() {
		return null;
	}

	@Override
	public String toString() {
		return inferredType.toString() + "{- inferred -}";
	}

}

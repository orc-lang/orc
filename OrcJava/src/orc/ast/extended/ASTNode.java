/**
 * Copyright (c) 2009, The University of Texas at Austin ("U. T. Austin")
 * All rights reserved.
 *
 * <p>You may distribute this file under the terms of the OSI Simplified BSD License,
 * as defined in the LICENSE file found in the project's top-level directory.
 */
package orc.ast.extended;

import orc.error.Located;
import orc.error.SourceLocation;

/**
 * @author jthywiss
 *
 */
public interface ASTNode extends Located {

	/* (non-Javadoc)
	 * @see orc.error.Located#getSourceLocation()
	 */
	public SourceLocation getSourceLocation();

	public <E> E accept(Visitor<E> visitor);

}

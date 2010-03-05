//
// ASTNode.java -- Java interface ASTNode
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

package orc.ast.extended;

import orc.ast.extended.visitor.Visitor;
import orc.error.Located;
import orc.error.SourceLocation;

/**
 *
 * @author jthywiss
 */
public interface ASTNode extends Located {

	/* (non-Javadoc)
	 * @see orc.error.Located#getSourceLocation()
	 */
	public SourceLocation getSourceLocation();

	public <E> E accept(Visitor<E> visitor);

}

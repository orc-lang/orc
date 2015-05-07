//
// Located.java -- Java interface Located
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

package orc.error;

/**
 * Interface implemented by any construct (syntax tree node, exception, etc)
 * which is associated with a particular location in the source program. 
 * 
 * @author dkitchin
 */
public interface Located {

	public SourceLocation getSourceLocation();

}

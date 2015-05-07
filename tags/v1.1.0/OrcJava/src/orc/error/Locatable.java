//
// Locatable.java -- Java interface Locatable
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
 * Interface representing entities whose source location can be
 * assigned, not just observed. Typically used when an entity has
 * a source location associated with it, but that location cannot
 * be determined at the point where the entity is created, and
 * so is assigned later (cf TokenException).
 * 
 * @author dkitchin
 */
public interface Locatable extends Located {

	public void setSourceLocation(SourceLocation location);

}

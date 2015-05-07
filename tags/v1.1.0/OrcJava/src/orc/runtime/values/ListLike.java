//
// ListLike.java -- Java interface ListLike
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

package orc.runtime.values;

import orc.runtime.Token;
import orc.runtime.sites.core.TryCons;
import orc.runtime.sites.core.TryNil;

public interface ListLike {
	/**
	 * Return the head and tail of a cons-like data structure to a token, or
	 * die. The only place this should be called is {@link TryCons}
	 */
	public void uncons(Token caller);

	/**
	 * Signal a token if this value is equivalent to nil, or die. The only place
	 * this should be called is {@link TryNil}
	 * 
	 * @param caller
	 */
	public void unnil(Token caller);
}

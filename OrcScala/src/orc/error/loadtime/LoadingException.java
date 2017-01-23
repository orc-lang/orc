//
// LoadingException.java -- Java class LoadingException
// Project OrcScala
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.error.loadtime;

import orc.error.OrcException;

/**
 * Exceptions generated while creating an execution graph from a portable
 * representation, in preparation for execution.
 *
 * @author dkitchin
 */

public abstract class LoadingException extends OrcException {
    private static final long serialVersionUID = 8704123769097479728L;

    public LoadingException(final String message) {
        super(message);
    }

}

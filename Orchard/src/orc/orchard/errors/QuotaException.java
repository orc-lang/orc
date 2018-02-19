//
// QuotaException.java -- Java class QuotaException
// Project Orchard
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.errors;

public class QuotaException extends Exception {
    private static final long serialVersionUID = 705960202403819731L;

    public QuotaException() {
    }

    public QuotaException(final String arg0) {
        super(arg0);
    }
}

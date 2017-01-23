//
// InvalidOilException.java -- Java class InvalidOilException
// Project Orchard
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.errors;

import java.util.List;

public class InvalidOilException extends ExceptionWithProblems {

    public InvalidOilException(final String message) {
        super(message);
    }

    public InvalidOilException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public InvalidOilException(final Throwable cause) {
        super(cause);
    }

    public InvalidOilException(final List<? extends OrcProgramProblem> problems) {
        super(problems);
    }

}

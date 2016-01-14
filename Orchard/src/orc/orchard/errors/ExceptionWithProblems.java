//
// ExceptionWithProblems.java -- Java class ExceptionWithProblems
// Project Orchard
//
// Created by jthywiss on Dec 12, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.errors;

import java.util.List;

/**
 * @author jthywiss
 */
public abstract class ExceptionWithProblems extends Exception {

    protected List<? extends OrcProgramProblem> problems = null;

    public ExceptionWithProblems() {
        super();
    }

    public ExceptionWithProblems(final String message) {
        super(message);
    }

    public ExceptionWithProblems(final Throwable cause) {
        super(cause);
    }

    public ExceptionWithProblems(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ExceptionWithProblems(final List<? extends OrcProgramProblem> problems) {
        super(problemsToString(problems));
        this.problems = problems;
    }

    protected static String problemsToString(final List<? extends OrcProgramProblem> problems) {
        final StringBuilder sb = new StringBuilder();
        for (final OrcProgramProblem p : problems) {
            sb.append(p.longMessage);
            sb.append("\n");
        }
        sb.deleteCharAt(sb.length() - 1); // Remove trailing newline
        return sb.toString();
    }

    public List<? extends OrcProgramProblem> getProblems() {
        return problems;
    }

    public void setProblems(final List<? extends OrcProgramProblem> problems) {
        this.problems = problems;
    }

}

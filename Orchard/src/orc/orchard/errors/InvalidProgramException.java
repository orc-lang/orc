//
// InvalidProgramException.java -- Java class InvalidProgramException
// Project Orchard
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.errors;

import java.util.ArrayList;
import java.util.List;

import orc.error.compiletime.CompilationException;
import orc.orchard.OrchardCompileLogger.CompileMessage;

public class InvalidProgramException extends InvalidOilException {

    public InvalidProgramException(final String message) {
        super(message);
    }

    public InvalidProgramException(final List<CompileMessage> compileMsgs) {
        super(compileMsgsToProblemArray(compileMsgs));
    }

    protected static List<OrcProgramProblem> compileMsgsToProblemArray(final List<CompileMessage> compileMsgs) {
        final List<OrcProgramProblem> ps = new ArrayList<OrcProgramProblem>(compileMsgs.size());
        for (final CompileMessage compileMsg : compileMsgs) {
            ps.add(new CompileProblem(compileMsg));
        }
        return ps;
    }

    public static class CompileProblem extends OrcProgramProblem {

        protected CompileProblem() {
            super();
        }

        public CompileProblem(final CompileMessage compileMsg) {
            super();
            this.severity = compileMsg.severity.ordinal();
            this.code = compileMsg.code;
            this.message = compileMsg.message;
            this.filename = compileMsg.position != null ? compileMsg.position.start().resource().descr() : "";
            this.line = compileMsg.position != null ? compileMsg.position.start().line() : -1;
            this.column = compileMsg != null ? compileMsg.position.start().column() : -1;
            if (compileMsg.position != null) {
                this.longMessage = compileMsg.position.toString() + ": " + message;
            } else {
                this.longMessage = message;
            }
            this.longMessage = compileMsg.longMessage();
            this.orcWikiHelpPageName = compileMsg.exception instanceof CompilationException ? compileMsg.exception.getClass().getSimpleName() : null;
        }

    }

}

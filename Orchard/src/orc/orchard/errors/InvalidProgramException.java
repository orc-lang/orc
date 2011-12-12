//
// InvalidProgramException.java -- Java class InvalidProgramException
// Project Orchard
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.errors;

import java.util.ArrayList;
import java.util.List;

import orc.compile.parse.PositionWithFilename;
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
			this.filename = compileMsg.position instanceof PositionWithFilename ? ((PositionWithFilename) compileMsg.position).filename() : "";
			this.line = compileMsg.position.line();
			this.column = compileMsg.position.column();
			this.longMessage = compileMsg.longMessage();
		}

	}

}

//
// OrcProgramProblem.java -- Java class OrcProgramProblem
// Project Orchard
//
// Created by jthywiss on Dec 12, 2011.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.errors;

/**
 * A problem in an Orc program. Similar to CompileMessage, but suitable for JAXB
 * marshaling.
 *
 * @author jthywiss
 */
public abstract class OrcProgramProblem {
    // FIXME: Accept a range, not a position
    public int severity;
    public int code;
    public String message;
    public String filename;
    public int line;
    public int column;
    public String longMessage;
    public String orcWikiHelpPageName;
}

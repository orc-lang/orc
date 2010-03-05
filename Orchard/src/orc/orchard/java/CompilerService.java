//
// CompilerService.java -- Java class CompilerService
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

package orc.orchard.java;

import java.util.logging.Logger;

import orc.ast.xml.Oil;
import orc.orchard.AbstractCompilerService;
import orc.orchard.errors.InvalidProgramException;

public class CompilerService extends AbstractCompilerService {
	public CompilerService() {
		super();
	}

	public CompilerService(final Logger logger) {
		super(logger);
	}

	public static void main(final String[] args) throws InvalidProgramException {
		final CompilerService c = new CompilerService();
		final Oil oil = c.compile("", "1 >x> x");
		System.out.println(oil.toXML());
	}
}

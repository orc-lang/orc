//
// CompilerService.java -- Java class CompilerService
// Project Orchard
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.java;

import orc.orchard.AbstractCompilerService;
import orc.orchard.errors.InvalidProgramException;

public class CompilerService extends AbstractCompilerService {
	public CompilerService() {
		super();
	}

	public static void main(final String[] args) throws InvalidProgramException {
		final CompilerService c = new CompilerService();
		final String oil = c.compile("", "1 >x> x");
		System.out.println(oil);
	}
}

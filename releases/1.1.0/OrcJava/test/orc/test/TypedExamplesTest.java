//
// TypedExamplesTest.java -- Java class TypedExamplesTest
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

package orc.test;

import junit.framework.Test;
import orc.Config;

/**
 * Run examples with type-checking enabled.
 *
 * @author quark
 */
public class TypedExamplesTest {
	public static Test suite() {
		final Config config = new Config();
		config.setTypeChecking(true);
		return ExamplesTest.buildSuite(config);
	}
}

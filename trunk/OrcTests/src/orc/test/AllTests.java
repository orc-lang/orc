//
// AllTests.java -- Java class AllTests
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test;

import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( { OrcParserTest.class, ExamplesTest.class, XMLExamplesTest.class, TypedExamplesTest.class, DocExamplesTest.class })
public class AllTests {
	public static void main(final String[] args) {
		JUnitCore.main("orc.test.AllTests");
	}
}

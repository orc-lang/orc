//
// OilTest -- Java class OilTest
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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import orc.Config;
import orc.Orc;
import orc.ast.xml.Oil;
import orc.error.compiletime.CompilationException;

import org.junit.Test;
import org.kohsuke.args4j.CmdLineException;

public class OilTest {
	@Test
	public void unmarshal() throws CompilationException, IOException, CmdLineException {
		final Config config = new Config();
		config.setInputFile(new File("examples/strcat.orc")); //Must have a file in config.  It can be bogus. :-)
		final Oil oil1 = new Oil(Orc.compile(new StringReader("1"), config));
		final String xml = oil1.toXML();
		System.out.println(xml);
		// TODO: verify the syntax of the XML;
		// for now we just check for exceptions
		final Oil oil2 = Oil.fromXML(xml);
		oil2.unmarshal(config);
		// TODO: verify the structure of the file
		// for now we just check for exceptions
	}
}

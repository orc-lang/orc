//
// OilTest.java -- Java class OilTest
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import orc.Config;
import orc.Orc;
import orc.ast.oil.expression.Expression;
import orc.ast.oil.visitor.SiteResolver;
import orc.ast.xml.Oil;
import orc.error.compiletime.CompilationException;

import org.junit.Test;
import org.kohsuke.args4j.CmdLineException;

public class OilTest {
	@Test
	public void testOilXml() throws CompilationException, IOException, CmdLineException {
		final Config config = new Config();
		config.setInputFile(new File("examples/misra_bst.orc")); //Must have a file in config.  It can be bogus (not run). :-)
		final Expression ast1 = Orc.compile(config);
		//System.out.println(ast1);
		final Oil oil1 = new Oil(ast1);
		final String xml = oil1.toXML();
		//System.out.println(xml);
		// TODO: verify the syntax of the XML;
		// for now we just check for exceptions
		final Oil oil2 = Oil.fromXML(xml);
		Expression ast2 = oil2.unmarshal(config);
		ast2 = SiteResolver.resolve(ast2, config);
		//System.out.println(ast2);
		//System.out.println("ast1 ?= ast2  "+ast1.equals(ast2));
		//System.out.println("ast1.toString() ?= ast2.toString()  "+ast1.toString().equals(ast2.toString()));
		assertEquals(ast1, ast2);
	}
}

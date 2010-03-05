//
// Argument.java -- Java class Argument
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

package orc.ast.xml.expression.argument;

import javax.xml.bind.annotation.XmlSeeAlso;

import orc.Config;
import orc.ast.xml.expression.Expression;
import orc.error.compiletime.CompilationException;

/**
 * Arguments to sites and expressions.
 * @author quark
 */
@XmlSeeAlso(value = { Constant.class, Field.class, Site.class, Variable.class })
public abstract class Argument extends Expression {
	@Override
	public abstract orc.ast.oil.expression.argument.Argument unmarshal(Config config) throws CompilationException;
}

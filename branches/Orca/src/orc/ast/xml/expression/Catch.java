//
// Catch.java -- Java class Catch
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

package orc.ast.xml.expression;

import javax.xml.bind.annotation.XmlElement;

import orc.Config;
import orc.error.compiletime.CompilationException;

public class Catch extends Expression {
	@XmlElement(required = true)
	public Expression tryExpr;
	@XmlElement(required = true)
	public Def catchHandler;

	public Catch() {
	}

	public Catch(final Def catchHandler, final Expression tryExpr) {
		this.tryExpr = tryExpr;
		this.catchHandler = catchHandler;
	}

	@Override
	public String toString() {
		return super.toString() + "try(" + tryExpr.toString() + ") catch" + catchHandler.toString();
	}

	@Override
	public orc.ast.oil.expression.Expression unmarshal(final Config config) throws CompilationException {
		return new orc.ast.oil.expression.Catch(catchHandler.unmarshal(config), tryExpr.unmarshal(config));
	}
}

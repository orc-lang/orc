//
// Expression.java -- Java class Expression
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

import java.io.Serializable;

import javax.xml.bind.annotation.XmlSeeAlso;

import orc.Config;
import orc.error.compiletime.CompilationException;

@XmlSeeAlso(value = { Parallel.class, Call.class, DeclareDefs.class, Stop.class, Pruning.class, Sequential.class, Otherwise.class, WithLocation.class, HasType.class, DeclareType.class })
public abstract class Expression implements Serializable {
	public abstract orc.ast.oil.expression.Expression unmarshal(Config config) throws CompilationException;
}

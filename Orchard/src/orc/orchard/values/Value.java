//
// Value.java -- Java class Value
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

package orc.orchard.values;

import javax.xml.bind.annotation.XmlSeeAlso;

import orc.ast.xml.expression.argument.Constant;
import orc.ast.xml.expression.argument.Field;
import orc.ast.xml.expression.argument.Site;

/**
 * Orc publishable values.
 * 
 * @author quark
 */
@XmlSeeAlso(value = { Constant.class, Field.class, Site.class, UnrepresentableValue.class, List.class, Tuple.class, Tagged.class })
public abstract class Value {
}

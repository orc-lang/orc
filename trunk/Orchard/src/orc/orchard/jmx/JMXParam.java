//
// JMXParam.java -- Java annotation type JMXParam
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

package orc.orchard.jmx;

import static java.lang.annotation.ElementType.PARAMETER;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provide the name for an MBean operation parameter.
 * @author quark
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ PARAMETER })
public @interface JMXParam {
	String value();
}

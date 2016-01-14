//
// JMXDescription.java -- Java annotation type JMXDescription
// Project Orchard
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.jmx;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provide the description for an MBean operation.
 * 
 * @author quark
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ CONSTRUCTOR, METHOD, PARAMETER, TYPE })
public @interface JMXDescription {
    String value();
}

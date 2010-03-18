//
// ChildNode.java -- Java class ChildNode
// Project OrcJava
//
// $Id$
//
// Created by jthywiss on Mar 18, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation indicating this field contains a reference to a child {@link AstNode},
 * or a collection of child <code>AstNode</code>s, or an array of child <code>AstNode</code>s.
 *
 * @see AstNode
 * @author jthywiss
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ChildNode {

}

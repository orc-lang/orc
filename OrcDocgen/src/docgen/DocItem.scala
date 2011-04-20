//
// DocItem.scala -- Scala trait DocItem and children
// Project OrcDocgen
//
// $Id$
//
// Created by dkitchin on Dec 17, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package docgen

/**
 *
 *
 * @author dkitchin
 */

// Parent trait of parsed documentation items
trait DocItem


// A plain chunk of documentation text
case class DocText(content: String) extends DocItem


// Parent of declared code elements (sites and defs)
trait DocDecl extends DocItem {

  val name: String
  val typeDeclaration: String
  val body: List[DocItem]
  val keyword: String

}

// @site
// Structured documentation of a site or method
case class DocSite(name: String, typeDeclaration: String, body: List[DocItem]) extends DocDecl {
  val keyword = "site"
}

// @def
// Documentation of a definition
case class DocDefn(name: String, typeDeclaration: String, body: List[DocItem]) extends DocDecl {
  val keyword = "def"
}


// @implementation
// Get the next chunk of code outside of a comment and use it as a code block here
case object DocImpl extends DocItem


// Text in between document comment blocks. This is not processed directly
// into XML, but is sometimes used to fill in a DocImpl
case class DocOutside(content: String) extends DocItem

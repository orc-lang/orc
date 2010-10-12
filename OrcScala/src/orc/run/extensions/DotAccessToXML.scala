//
// DotAccessToXML.scala -- Scala class/trait/object DotAccessToXML
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Sep 27, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import scala.collection.immutable.List
import scala.xml._
import orc.OrcOptions
import orc.TokenAPI
import orc.values.Field
import orc.ast.oil.nameless.Expression

/**
 * 
 *
 * @author dkitchin
 */
trait DotAccessToXML extends InvocationBehavior {

  override def invoke(t: TokenAPI, v: AnyRef, vs: List[AnyRef]): Unit = { 
    
    v match {
      case xml : Elem => {
        val target = 
          vs match {
            case List(Field(f)) => Some(f)
            case List(s : String) => Some(s)
            case _ => None
          }
        target match {
          case Some(child) => {
            val nodes = (xml \ child)
            if (!(nodes.isEmpty)) {
              t.publish(nodes.toList)
            }
            else {
              xml.attribute(child) match {
                case Some(nodes) => t.publish(nodes.toList)
                case None => t.halt
              }
            }
          }
          case None => t.halt
        }
      }
      case _ => super.invoke(t, v, vs)
    }
    
    
  }

}
//
// SupportForXMLInvocation.scala -- Scala class/trait/object SupportForXMLInvocation
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jan 24, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.InvocationBehavior
import orc.Handle

/**
 * 
 *
 * @author dkitchin
 */
trait SupportForXMLInvocation extends InvocationBehavior {
  
  override def invoke(h: Handle, v: AnyRef, vs: List[AnyRef]) { 
    v match {
      case xml: scala.xml.Elem => {
        vs match {
          case List(orc.values.Field(f)) => {
            xml.attributes.get(f) match {
              case Some(v) => h.publish(v)
              case None => h.halt
            }
          }
          case List(s: String) => {
            h.publish((xml \ s).toList)
          }
          case _ => super.invoke(h, v, vs)
        }
      }
      case _ => super.invoke(h, v, vs)
    }
  }

}
//
// InvocationBehavior.scala -- Scala class/trait/object InvocationBehavior
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.OrcRuntime
import orc.TokenAPI
import orc.error.runtime.UncallableValueException
import orc.error.OrcException
import orc.error.runtime.JavaException
import orc.values._
import orc.values.sites.JavaCall
import orc.values.sites.Site


/**
 * 
 *
 * @author dkitchin
 */
trait InvocationBehavior extends OrcRuntime {
  /* By default, an invocation halts silently. This will be overridden by other traits. */
  def invoke(t: TokenAPI, v: AnyRef, vs: List[AnyRef]): Unit = { t.halt }
}

trait ErrorOnUndefinedInvocation extends InvocationBehavior {
  /* This replaces the default behavior because it does not call super */
  override def invoke(t: TokenAPI, v: AnyRef, vs: List[AnyRef]) {
    val error = "You can't call the "+(if (v != null) v.getClass().toString() else "null")+" \" "+Format.formatValue(v)+" \""
    t !! new UncallableValueException(error)
  }
}


trait SupportForJavaObjectInvocation extends InvocationBehavior {
  
  override def invoke(t: TokenAPI, v: AnyRef, vs: List[AnyRef]) { 
    v match {
      case v : OrcValue => super.invoke(t, v, vs)
      case _ => JavaCall(v, vs, t)
    }
  }

}
  

trait SupportForSiteInvocation extends InvocationBehavior {  
  override def invoke(t: TokenAPI, v: AnyRef, vs: List[AnyRef]) {
    v match {
      case (s: Site) => 
        try {
          s.call(vs, t)
        }
        catch {
          case e: OrcException => t !! e
          case e: InterruptedException => throw e
          case e: Exception => t !! new JavaException(e) //FIXME: This seems risky
        }
      case _ => super.invoke(t, v, vs)
    }
  }
}

trait SupportForXMLInvocation extends InvocationBehavior {
  
  override def invoke(t: TokenAPI, v: AnyRef, vs: List[AnyRef]) { 
    v match {
      case xml: scala.xml.Elem => {
        vs match {
          case List(orc.values.Field(f)) => {
            xml.attributes.get(f) match {
              case Some(v) => t.publish(v)
              case None => t.halt
            }
          }
          case List(s: String) => {
            t.publish((xml \ s).toList)
          }
          case _ => super.invoke(t, v, vs)
        }
      }
      case _ => super.invoke(t, v, vs)
    }
  }

}

//
// AnnotationHack.scala -- Scala object AnnotationHack and annotations to use with it.
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.orctimizer

import scala.reflect.ClassTag
import orc.ast.orctimizer.named._
import orc.values.sites.DirectSite
import orc.DirectInvoker
import orc.OrcRuntime
import orc.values.sites.Effects
import orc.values.Signal

class OrcAnnotation extends DirectSite {
  override def publications: orc.values.sites.Range = orc.values.sites.Range(1, 1)
  override def timeToPublish: orc.values.sites.Delay = orc.values.sites.Delay.NonBlocking
  override def timeToHalt: orc.values.sites.Delay = orc.values.sites.Delay.NonBlocking
  override def effects: Effects = Effects.BeforePub

  class Invoker extends DirectInvoker {
    def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = true
    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = Signal
  }

  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]) = new Invoker
}

// FIXME: Sequentialize causes objects to fail to ever fill their fields. This results in what appears to be deadlock.
class Sequentialize extends OrcAnnotation

class SinglePublication extends OrcAnnotation

object AnnotationHack {
  def isAnnotationForced[A <: OrcAnnotation : ClassTag]: Boolean = {
    val TargetAnnotation = implicitly[ClassTag[A]]
    val annotationName = TargetAnnotation.runtimeClass.getName
    System.getProperty(annotationName, "") match {
      case "force" =>
        true
      case _ =>
        false
    }
  }

  def inAnnotation[A <: OrcAnnotation : ClassTag](e: Expression.Z): Boolean = {
    val TargetAnnotation = implicitly[ClassTag[A]]
    e.parents exists {
      case _ if isAnnotationForced[A] =>
        true
      case Branch.Z(Call.Z(Constant.Z(TargetAnnotation(_)), _, _), _, _) =>
        true
      case _ =>
        false
    }
  }
}

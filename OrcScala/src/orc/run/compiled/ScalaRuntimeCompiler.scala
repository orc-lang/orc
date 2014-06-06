//
// ScalaRuntimeCompiler.scala -- Scala class/trait/object ScalaRuntimeCompiler
// Project OrcScala
//
// $Id$
//
// Created by amp on May 21, 2014.
//
// Copyright (c) 2014 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.compiled

import orc.ast.porc._
import scala.tools.reflect.ToolBox

/**
  * @author amp
  */
class ScalaRuntimeCompiler extends ScalaCodeGen {
  import scala.reflect.runtime.{ universe => ru }
  import scala.reflect.runtime._
  import ru.Tree

  val cm = ru.runtimeMirror(getClass.getClassLoader)
  val tb = cm.mkToolBox()
  val build = ru.build

  def compile(e: Expr): OrcModule = {
    val (code, bindings) = this(e)
    compile(code, bindings)
  }
  
  def compile(code: String, bindings: Map[String, AnyRef]): OrcModule = {
    val p = tb.parse(code)

    tb.eval(bind(bindings, p)).asInstanceOf[OrcModule]
  }

  def bind(bindings: Map[String, AnyRef], tree: ru.Tree): ru.Tree = {
    class Substituter extends ru.Transformer {
      import ru.{build => _, _}

      override def transform(tree: Tree): Tree = tree match {
        case Ident(name) if bindings contains name.decoded =>
          val v = bindings(name.decoded)
          Ident(build.setTypeSignature(build.newFreeTerm(name.decoded, v), typeOf[AnyRef]))       
        case _ =>
          super.transform(tree)
      }
    }

    return (new Substituter).transform(tree)
  }
}
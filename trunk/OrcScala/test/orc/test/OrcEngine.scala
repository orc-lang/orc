//
// OrcEngine.scala -- Scala class OrcEngine
// Project OrcScala
//
// $Id$
//
// Created by amshali on May 26, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package test.orc

import orc.run.StandardOrcRuntime
import orc.error.OrcException
import orc.oil.nameless.Expression
import orc.values.Format
import orc.values.sites.Site
import java.lang.StringBuffer

class OrcEngine {

  val out = new StringBuffer("")

  val orcRuntime = new StandardOrcRuntime {
    override def printToStdout(s: String) { print(s); out.append(s) }
    override def caught(e: Throwable) {
      e match {
        case oe: OrcException => out.append("Error: "+oe.getClass().getName()+": "+oe.getMessageOnly()+"\n")
        case _ => out.append("Error: "+e.getClass().getName()+": "+e.getMessage()+"\n") 
      }
      super.caught(e)
    }
  }
  
  def stop() = orcRuntime.stop

  def getOut() : StringBuffer = out

  def run(e : Expression) {    
    def k(v: AnyRef) {
      val vf = Format.formatValue(v)
      println(vf)
      out.append(vf + "\n") 
    }
    orcRuntime.runSynchronous(e, k)
  }

}

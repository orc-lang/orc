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

import orc.run.Orc
import orc.run.StandardOrcExecution
import orc.oil.nameless.Expression
import orc.values.Value
import orc.values.sites.Site
import java.lang._

class OrcEngine {

  val out = new StringBuffer("")

  val orcRuntime = new StandardOrcExecution {
    def emit(v: Value) { println(v); out.append(v.toOrcSyntax()+"\n") }
    //def halted { print("Done. \n") }
    //def invoke(t: this.Token, s: Site, vs: List[Value]) { s.call(vs,t) }
    override def expressionPrinted(s: String) { print(s); out.append(s) }
    //def caught(e: Throwable) { Console.err.println("Error: " + e.getMessage()); out.append("Error: " + e.getMessage()) } // for test cases with expected exceptions
    override def caught(e: Throwable) { throw e } // for debugging (will fail test cases with expected exceptions)
    
    // Currently blows the stack (or causes Rtimer to fail) on many examples, 
    // but switching to the actor version causes very bizarre failures.
    override def schedule(ts: List[Token]) { for (t <- ts) t.run }
  }

  def getOut() : StringBuffer = out

  def run(e : Expression) {
    orcRuntime.run(e)
  }

}

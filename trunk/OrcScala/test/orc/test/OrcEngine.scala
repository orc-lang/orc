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
import orc.oil.nameless.Expression
import orc.values.Literal
import orc.values.Value
import orc.values.sites.Site
import java.lang._

class OrcEngine {

  val out = new StringBuffer("")

  val orcRuntime = new Orc {
    def emit(v: Value) { println(v); out.append(v.toOrcSyntax()+"\n") }
    def halted { print("Done. \n") }
    def invoke(t: this.Token, s: Site, vs: List[Value]) { s.call(vs,t) }
    def expressionPrinted(s: String) { print(s); out.append(s) }
    def schedule(ts: List[Token]) { for (t <- ts) t.run }
  }

  def getOut() : StringBuffer = out

  def run(e : Expression) {
    orcRuntime.run(e)
  }

}

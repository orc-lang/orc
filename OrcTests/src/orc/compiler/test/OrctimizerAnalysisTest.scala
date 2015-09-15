//
// OrctimizerAnalysisTest.scala -- OrctimizerAnalysisTest class to test orc.compiler.orctimizer.Analysis
// Project OrcTest
//
// $Id$
//
// Created by amp on Sept 14, 2015.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compiler.test

import org.junit.Test
import org.junit.Assert._
import orc.ast.orctimizer.named._
import orc.compile.orctimizer._
import orc.values.sites.Delay
import orc.values.sites.Effects
import orc.lib.state.NewFlag
import orc.lib.builtin.Iff
import orc.values.Signal

/**
 * @author amp
 */
class OrctimizerAnalysisTest {
  val analyzer = new ExpressionAnalyzer
  import analyzer.ImplicitResults._
  
  def getInContext(expr: Expression, f: Expression) = {
    var res: Option[WithContext[Expression]] = None
    val searcher = new ContextualTransform.NonDescending {
      override def onExpression(implicit ctx: TransformContext) = {
        case e if e == f => res = Some(e in ctx); e
      }
    }
    searcher(expr)
    res.getOrElse {
      throw new IllegalArgumentException("f must be a subexpression of expr. Your test is buggy.")
    }
  }
  
  lazy val unanalyzableCall = Call(Constant(new orc.lib.net.BingSearchFactoryUsernameKey), List(), None)
  
  @Test
  def analyzeStop(): Unit = {
    val f = Stop()
    val a = getInContext(f, f)
    assertEquals(Delay.NonBlocking, expressionCtxWithResults(a).timeToHalt)
    assertEquals(Delay.Forever, a.timeToPublish)
    assertEquals(Effects.None, a.effects)
    assertEquals(Range(0, 0), a.publications)
  }
  
  @Test
  def analyzeConstant(): Unit = {
    val f = Constant("")
    val a = getInContext(f, f)
    assertEquals(Delay.NonBlocking, a.timeToHalt)
    assertEquals(Delay.NonBlocking, a.timeToPublish)
    assertEquals(Effects.None, a.effects)
    assertEquals(Range(1, 1), a.publications)
    assertEquals(Set(), a.freeVars)
  }

  @Test
  def analyzeSiteCall(): Unit = {
    val f = Call(Constant(NewFlag), List(), None)
    val a = getInContext(f, f)
    assertEquals(Delay.NonBlocking, a.timeToHalt)
    assertEquals(Delay.NonBlocking, a.timeToPublish)
    assertEquals(Effects.None, a.effects)
    assertEquals(Range(1, 1), a.publications)
  }

  @Test
  def analyzeVariableConstant(): Unit = {
    val x = new BoundVar(Some("important"))
    val f = Constant("") > x > x
    val a = getInContext(f, x)
    assertEquals(Delay.NonBlocking, a.timeToHalt)
    assertEquals(Delay.NonBlocking, a.timeToPublish)
    assertEquals(Effects.None, a.effects)
    assertEquals(Range(1, 1), a.publications)
  }
  
  @Test
  def analyzeFuture(): Unit = {
    val f = Future(Call(Constant(new orc.lib.net.BingSearchFactoryUsernameKey), List(), None))
    val a = getInContext(f, f)
    assertEquals(Delay.Blocking, a.timeToHalt)
    assertEquals(Delay.NonBlocking, a.timeToPublish)
    assertEquals(Effects.Anytime, a.effects)
    assertEquals(Range(1, 1), a.publications)
  }

  @Test
  def analyzeForceBlocking(): Unit = {
    val x = new BoundVar(Some("x"))
    val f = Force(x)
    val e = Future(Call(Constant(new orc.lib.net.BingSearchFactoryUsernameKey), List(), None)) > x > f
    val a = getInContext(e, f)
    assertEquals(Delay.Blocking, a.timeToHalt)
    assertEquals(Delay.Blocking, a.timeToPublish)
    assertEquals(Effects.None, a.effects)
    assertEquals(Range(0, 1), a.publications)
    assertEquals(Set(x), a.freeVars)
    assertEquals(ForceType.Immediately(true), a.forces(x))
  }

  @Test
  def analyzeForceNonBlocking(): Unit = {
    val x = new BoundVar(Some("x"))
    val f = Force(x)
    val e = Future(Constant("")) > x > f
    val a = getInContext(e, f)
    assertEquals(Delay.NonBlocking, a.timeToHalt)
    assertEquals(Delay.NonBlocking, a.timeToPublish)
    assertEquals(Effects.None, a.effects)
    assertEquals(Range(1, 1), a.publications)
    assertEquals(Set(x), a.freeVars)
    assertEquals(ForceType.Immediately(true), a.forces(x))
  }
  
  @Test
  def analyzeForceLater(): Unit = {
    val x = new BoundVar(Some("x"))
    val f = unanalyzableCall >> Force(x)
    val e = Future(Constant("")) > x > f
    val a = getInContext(e, f)
    assertEquals(Range(0, None), a.publications)
    assertEquals(Set(x), a.freeVars)
    assertEquals(ForceType.Eventually, a.forces(x))
  }
  
  @Test
  def analyzeLimit(): Unit = {
    val e = Limit(unanalyzableCall)
    val a = getInContext(e, e)
    assertEquals(Range(0, 1), a.publications)
    assertEquals(Effects.BeforePub, a.effects)
  }
  
  @Test
  def analyzePar(): Unit = {
    val x = new BoundVar(Some("x"))
    val f = Call(Constant(Iff), List(x), None) || Call(Constant(Iff), List(x), None) || Stop()
    val e = unanalyzableCall > x > f
    val a = getInContext(e, f)
    assertEquals(Range(0, 2), a.publications)
    assertEquals(Effects.None, a.effects)
    assertEquals(Delay.Blocking, a.timeToHalt)
    assertEquals(Delay.Blocking, a.timeToPublish)
  }
  
  @Test
  def analyzeParPubs(): Unit = {
    val x = new BoundVar(Some("x"))
    val f = Call(Constant(Iff), List(x), None) || Constant(Signal) || Stop()
    val e = unanalyzableCall > x > f
    val a = getInContext(e, f)
    assertEquals(Range(1, 2), a.publications)
    assertEquals(Effects.None, a.effects)
    assertEquals(Delay.Blocking, a.timeToHalt)
    assertEquals(Delay.NonBlocking, a.timeToPublish)
  }
  
  @Test
  def analyzeStopSeq(): Unit = {
    val e = Stop() >> unanalyzableCall
    val a = getInContext(e, e)
    assertEquals(Range(0, 0), a.publications)
    assertEquals(Effects.None, a.effects)
    assertEquals(Delay.NonBlocking, a.timeToHalt)
    assertEquals(Delay.Forever, a.timeToPublish)
  }
}

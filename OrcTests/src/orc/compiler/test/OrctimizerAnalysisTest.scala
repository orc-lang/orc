//
// OrctimizerAnalysisTest.scala -- OrctimizerAnalysisTest class to test orc.compiler.orctimizer.Analysis
// Project OrcTest
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
import orc.values.sites.Range
import orc.lib.state.NewFlag
import orc.lib.builtin.Iff
import orc.values.Signal
import orc.values.Field
import orc.lib.builtin.structured.TupleConstructor
import orc.lib.builtin.structured.RecordConstructor

/**
 * @author amp
 */
class OrctimizerAnalysisTest {
  val analyzer = new ExpressionAnalyzer
  import analyzer.ImplicitResults._
  
  def getInContext(expr: Expression, f: Expression) = {
    println(s"Testing analysis of $f in:\n$expr")
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
  
  lazy val unanalyzableCall = CallSite(Constant(new orc.lib.net.BingSearchFactoryUsernameKey), List(), None)
  
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
    val f = CallSite(Constant(NewFlag), List(), None)
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
  
  /*
  
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
    assertEquals(Range(0, 1), a.publications)
    assertEquals(Set(x), a.freeVars)
    assertEquals(ForceType.Eventually, a.forces(x))
  }
  */
  
  @Test
  def analyzeLimit(): Unit = {
    val e = Trim(unanalyzableCall || unanalyzableCall)
    val a = getInContext(e, e)
    assertEquals(Range(0, 1), a.publications)
    assertEquals(Effects.BeforePub, a.effects)
  }
  
  /*
  @Test
  def analyzePar(): Unit = {
    val x = new BoundVar(Some("x"))
    val f = Call(Constant(Iff), List(x), None) || Call(Constant(Iff), List(x), None) || Stop()
    val e = Future(unanalyzableCall) > x > f
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
    val e = Future(unanalyzableCall) > x > f
    val a = getInContext(e, f)
    assertEquals(Range(1, 2), a.publications)
    assertEquals(Effects.None, a.effects)
    assertEquals(Delay.Blocking, a.timeToHalt)
    assertEquals(Delay.NonBlocking, a.timeToPublish)
    assertEquals(ForceType.Immediately(false), a.forces(x))
  }
  
  @Test
  def analyzeParForce(): Unit = {
    val x = new BoundVar(Some("x"))
    val f = Constant(Signal) || Force(x)
    val e = Future(unanalyzableCall) > x > f
    val a = getInContext(e, f)
    assertEquals(Range(1, 2), a.publications)
    assertEquals(Effects.None, a.effects)
    assertEquals(Delay.Blocking, a.timeToHalt)
    assertEquals(Delay.NonBlocking, a.timeToPublish)
    assertEquals(ForceType.Immediately(false), a.forces(x))
  }
  
  @Test
  def analyzeSeqForce(): Unit = {
    val x = new BoundVar(Some("x"))
    val f = unanalyzableCall >> Force(x)
    val e = Future(unanalyzableCall) > x > f
    val a = getInContext(e, f)
    assertEquals(Range(0, 1), a.publications)
    assertEquals(Effects.Anytime, a.effects)
    assertEquals(Delay.Blocking, a.timeToHalt)
    assertEquals(Delay.Blocking, a.timeToPublish)
    assertEquals(ForceType.Eventually, a.forces(x))
  }
  */
  
  @Test
  def analyzeStopSeq(): Unit = {
    val e = Stop() >> unanalyzableCall
    val a = getInContext(e, e)
    assertEquals(Range(0, 0), a.publications)
    assertEquals(Effects.None, a.effects)
    assertEquals(Delay.NonBlocking, a.timeToHalt)
    assertEquals(Delay.Forever, a.timeToPublish)
  }

  
  /*
  @Test
  def analyzeDefCall(): Unit = {
    val x = new BoundVar(Some("x"))
    val id = new BoundVar(Some("id"))
    val body = id
    val f = CallDef(id, List(Constant(BigInt(1))), None)
    val e = DeclareDefs(List(Def(id, List(x), body, List(), None, None)), f)
    val a = getInContext(e, f)
    a.publications
    // TODO: Currently this doesn't know anything about the properties of id.
    // So nothing worth testing here.
  }
  
  @Test
  def analyzeDefPub(): Unit = {
    val x = new BoundVar(Some("x"))
    val id = new BoundVar(Some("id"))
    val body = x
    val f = Force(id)
    val e = DeclareDefs(List(Def(id, List(x), body, List(), None, None)), f)
    val a = getInContext(e, f)
    assertEquals(Range(1, 1), a.publications)
    assertEquals(Effects.None, a.effects)
    assertEquals(Delay.NonBlocking, a.timeToHalt)
    assertEquals(Delay.NonBlocking, a.timeToPublish)
  }
  
  @Test
  def analyzeClosurePub(): Unit = {
    val x = new BoundVar(Some("x"))
    val y = new BoundVar(Some("y"))
    val id = new BoundVar(Some("id"))
    val body = y
    val f = Force(id)
    val e = Future(unanalyzableCall) > y > DeclareDefs(List(Def(id, List(x), body, List(), None, None)), f)
    val a = getInContext(e, f)
    assertEquals(Range(1, 1), a.publications)
    assertEquals(Effects.None, a.effects)
    assertEquals(Delay.Blocking, a.timeToHalt)
    assertEquals(Delay.Blocking, a.timeToPublish)
  }
  
  @Test
  def analyzeClosureSiteCall(): Unit = {
    val x = new BoundVar(Some("x"))
    val y = new BoundVar(Some("y"))
    val id = new BoundVar(Some("id"))
    val body = y
    val f = Call(Constant(TupleConstructor), List(Constant(Field("apply")), id), None)
    val e = Future(Constant("")) > y > DeclareDefs(List(Def(id, List(x), body, List(), None, None)), f)
    val a = getInContext(e, f)
    assertEquals(Range(1, 1), a.publications)
    assertEquals(Delay.NonBlocking, a.timeToHalt)
    assertEquals(Delay.NonBlocking, a.timeToPublish)
  }
  
  @Test
  def analyzeConcatSimple(): Unit = {
    val e = Concat(unanalyzableCall, Constant(""))
    val a = getInContext(e, e)
    assertEquals(Range(1, 2), a.publications)
    assertEquals(Effects.Anytime, a.effects)
    assertEquals(Delay.Blocking, a.timeToHalt)
    assertEquals(Delay.Blocking, a.timeToPublish)
  }
  
  @Test
  def analyzeConcatFast(): Unit = {
    val e = Concat(Call(Constant(Iff), List(Constant("")), None), Constant(""))
    val a = getInContext(e, e)
    assertEquals(Range(1, 2), a.publications)
    assertEquals(Effects.None, a.effects)
    assertEquals(Delay.NonBlocking, a.timeToHalt)
    assertEquals(Delay.NonBlocking, a.timeToPublish)
  }
  */
  
  @Test
  def analyzeComplex1(): Unit = {
    val v1541 = new BoundVar()
    val v1542 = new BoundVar()
    val e = (CallSite(Constant(TupleConstructor), List(Constant(Field("apply")), Constant("")), None) > v1541 > 
      (CallSite(Constant(TupleConstructor), List(Constant(Field("unapply")), Constant("")), None) > v1542 > 
      (CallSite(Constant(RecordConstructor), List(v1541, v1542), None) >> Stop())))
    val a = getInContext(e, e)
    assertEquals(Range(0, 0), a.publications)
    assertEquals(Effects.None, a.effects)
    assertEquals(Delay.NonBlocking, a.timeToHalt)
    assertEquals(Delay.Forever, a.timeToPublish)
  }
  
  /*
  @Test
  def analyzeComplex2(): Unit = {
    val x = new BoundVar(Some("x"))
    val id = new BoundVar(Some("id"))
    val id2 = new BoundVar(Some("id"))
    val body = x
    val defs = List(Def(id, List(x), body, List(), None, None), Def(id2, List(x), body, List(), None, None))
    val v1541 = new BoundVar()
    val v1542 = new BoundVar()
    val f = (CallSite(Constant(TupleConstructor), List(Constant(Field("apply")), id), None) > v1541 > 
      (CallSite(Constant(TupleConstructor), List(Constant(Field("unapply")), id2), None) > v1542 > 
      (CallSite(Constant(RecordConstructor), List(v1541, v1542), None) >> Stop())))
    val e = DeclareDefs(defs, f)
    val a = getInContext(e, f)
    assertEquals(Range(0, 0), a.publications)
    assertEquals(Effects.None, a.effects)
    assertEquals(Delay.NonBlocking, a.timeToHalt)
    assertEquals(Delay.Forever, a.timeToPublish)
  }
  */
  
  // TODO: test timeToHalt for: 
  // (Tuple(.apply, toattr) >`v1541> (Tuple(.unapply, fromattr) >`v1542> (Record(`v1541, `v1542) >`ov1784> stop)))
}


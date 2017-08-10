//
// Statistics.scala -- Scala object Statistics
// Project OrcScala
//
// Created by amp on Aug 03, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.porc

import orc.util.AutoFileDataOutput

object Statistics {
  /** Collect the direct nodes in `ast`.
   *  
   *  @return the set of direct nodes and the set of nested expressions.
   */
  def collectDirect(ast: PorcAST.Z): (Set[PorcAST.Z], Set[PorcAST.Z])  = {
    val nested: Set[PorcAST.Z] = ast match {
      case Continuation.Z(args, body) => Set(ast)
      case Method.Z(name, isRoutine, args, body) => Set(ast)
      case _ => Set()
    }
    
    val direct = (ast.subtrees.toSet) -- nested.flatMap(_.subtrees)
    
    val (directs, nesteds) = (direct.map(collectDirect) + ((direct, nested))).unzip
    
    (directs.flatten + ast, nesteds.flatten)
  }

  /** Collect the direct nodes in every closure body in `ast`.
   *  
   */
  def collect(ast: PorcAST.Z): Map[PorcAST.Z, Set[PorcAST.Z]]  = {
    val (direct, nested) = collectDirect(ast)
    val nestedMaps = nested.flatMap(a => collect(a.subtrees.head))
    (nestedMaps + (ast -> direct)).toMap
  }
  
  val tags = Set(
      classOf[HaltToken] -> 'ExecutionControl,
      classOf[NewToken] -> 'ExecutionControl,
      classOf[NewCounter] -> 'ExecutionControl,
      classOf[NewTerminator] -> 'ExecutionControl,
      classOf[CheckKilled] -> 'ExecutionControl,
      classOf[Kill] -> 'ExecutionControl,
      classOf[GetField] -> 'DataFlow,
      classOf[GetMethod] -> 'DataFlow,
      classOf[Force] -> 'DataFlow,
      classOf[Bind] -> 'DataFlow,
      classOf[BindStop] -> 'DataFlow,
      classOf[CallContinuation] -> 'DataFlow,
      classOf[New] -> 'Computation,
      classOf[MethodCPSCall] -> 'Computation,
      classOf[MethodDirectCall] -> 'Computation,
      classOf[Let] -> 'Free,
      classOf[Argument] -> 'Free,
      classOf[Sequence] -> 'Free,
      classOf[PorcAST] -> 'All,
      )
  
  def count(asts: Set[PorcAST.Z]): Map[Symbol, Int] = {
    val foundTags = for(a <- asts.toSeq; (c, t) <- tags if c.isInstance(a.value)) yield t
    foundTags.groupBy(identity).mapValues(_.size).view.force.withDefaultValue(0)
  }
    
  
  def percent(f: Double) = {
    (f * 100).formatted("%.1f%%")
  }
  
  def apply(ast: PorcAST) = {
    val dataout = new AutoFileDataOutput("porc_statistics", true)
    
    val rootdatalogger = dataout.logger(Seq("program" -> ast.sourceTextRange.toString))
    val m = collect(ast.toZipper)
    for((a, direct) <- m) {
      val datalogger = rootdatalogger.subLogger(Seq("closure" -> a.value.prettyprintWithoutNested().take(100).replace("\n", " ").replace("\t", " ")))
      
      //println(direct.map(_.value.prettyprintWithoutNested()).mkString("=======\n","\n---\n","\n======="))
      val counts = count(direct)
      //println(counts)
      val computation = counts('Computation)
      val dataFlow = counts('DataFlow)
      val execution = counts('ExecutionControl)
      val nonfree = counts('All) - counts('Free)
      
      datalogger.log("computation", computation)
      datalogger.log("dataFlow", dataFlow)
      datalogger.log("executionControl", execution)
      datalogger.log("nonfree", nonfree)
      
      Logger.fine(s"""
        |${a.value.prettyprintWithoutNested()}
        |-------
        |computation = $computation, data flow = $dataFlow, execution control = $execution, nonfree = $nonfree,
        |computation fract = ${percent(computation.toFloat / nonfree)}, data flow fract = ${percent(dataFlow.toFloat / nonfree)}, work fract = ${percent((dataFlow.toFloat + computation) / nonfree)}
      """.stripMargin.stripLineEnd)
    }
    
    dataout.close()
  }
}
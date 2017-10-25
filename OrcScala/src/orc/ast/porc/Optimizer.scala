//
// Optimizer.scala -- Scala class Optimizer
// Project OrcScala
//
// Created by amp on Jun 3, 2013.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.porc

import orc.compile.{ CompilerOptions, NamedOptimization, OptimizerStatistics }

import swivel.Zipper

trait Optimization extends ((Expression.Z, AnalysisProvider[PorcAST]) => Option[Expression]) with NamedOptimization {
  //def apply(e : Expression, analysis : ExpressionAnalysisProvider[Expression], ctx: OptimizationContext) : Expression = apply((e, analysis, ctx))
  val name: String
}

case class Opt(name: String)(f: PartialFunction[(Expression.Z, AnalysisProvider[PorcAST]), Expression]) extends Optimization {
  def apply(e: Expression.Z, analysis: AnalysisProvider[PorcAST]): Option[Expression] = f.lift((e, analysis))
}
case class OptFull(name: String)(f: (Expression.Z, AnalysisProvider[PorcAST]) => Option[Expression]) extends Optimization {
  def apply(e: Expression.Z, analysis: AnalysisProvider[PorcAST]): Option[Expression] = f(e, analysis)
}

/** @author amp
  */
case class Optimizer(co: CompilerOptions) extends OptimizerStatistics {
  def apply(e: PorcAST, analysis: AnalysisProvider[PorcAST]): PorcAST = {
    val trans = new Transform {
      override def onExpression = {
        case (e: Expression.Z) => {
          val e1 = opts.foldLeft(e)((e, opt) => {
            opt(e, analysis) match {
              case None => e
              case Some(e2) =>
                if (e.value != e2) {
                  import orc.util.StringExtension._
                  Logger.finer(s"${opt.name}: ${e.value.toString.truncateTo(60)}\n====>\n${e2.toString.truncateTo(60)}")
                  countOptimization(opt)
                  e.replace(e.value ->> e2)
                } else
                  e
            }
          })
          e1.value
        }
      }
    }

    trans(e.toZipper())
  }

  import Optimizer._

  def newName(x: Variable) = {
    new Variable(x.optionalName.map(_ + "i"))
  }

  def renameVariables(e: Method.Z)(implicit mapping: Map[Variable, Variable]): Method = {
    e.value ->> (e match {
      case MethodCPS.Z(name, p, c, t, isDef, args, b) =>
        val newArgs = args.map(newName)
        val (newP, newC, newT) = (newName(p), newName(c), newName(t))
        MethodCPS(mapping(name), newP, newC, newT, isDef, newArgs, renameVariables(b)(mapping + ((p, newP)) + ((c, newC)) + ((t, newT)) ++ (args zip newArgs)))
      case MethodDirect.Z(name, isDef, args, b) =>
        val newArgs = args.map(newName)
        MethodDirect(mapping(name), isDef, newArgs, renameVariables(b)(mapping ++ (args zip newArgs)))
    })
  }

  def renameVariables(e: Argument.Z)(implicit mapping: Map[Variable, Variable]): Argument = {
    e.value ->> (e.value match {
      case v: Variable =>
        mapping.getOrElse(v, v)
      case a =>
        a
    })
  }

  def renameVariables(e: Expression.Z)(implicit mapping: Map[Variable, Variable]): Expression = {
    e.value ->> (e match {
      case Let.Z(x, v, b) =>
        val newX = newName(x)
        Let(newX, renameVariables(v), renameVariables(b)(mapping + ((x, newX))))
      case Continuation.Z(args, b) =>
        val newArgs = args.map(newName).view.force
        Continuation(newArgs, renameVariables(b)(mapping ++ (args zip newArgs)))
      case MethodDeclaration.Z(t, methods, b) =>
        val newMethodNames = methods.map(m => newName(m.name))
        val newMapping = mapping ++ (methods.map(_.name) zip newMethodNames)
        val newMethods = methods.map(m => renameVariables(m)(newMapping)).view.force
        MethodDeclaration(renameVariables(t), newMethods, renameVariables(b)(newMapping))
      case a: Argument.Z =>
        renameVariables(a)
      case CallContinuation.Z(t, args) =>
        CallContinuation(renameVariables(t), args.map(renameVariables).view.force)
      case Force.Z(p, c, t, futs) =>
        Force(renameVariables(p), renameVariables(c), renameVariables(t), futs.map(renameVariables).view.force)
      case Resolve.Z(p, c, t, futs) =>
        Resolve(renameVariables(p), renameVariables(c), renameVariables(t), futs.map(renameVariables).view.force)
      case Sequence.Z(exprs) =>
        Sequence(exprs.map(renameVariables))
      case MethodCPSCall.Z(external, target, p, c, t, args) =>
        MethodCPSCall(external, renameVariables(target), renameVariables(p), renameVariables(c), renameVariables(t), args.map(renameVariables).view.force)
      case MethodDirectCall.Z(external, target, args) =>
        MethodDirectCall(external, renameVariables(target), args.map(renameVariables).view.force)
      case IfLenientMethod.Z(a, left, right) =>
        IfLenientMethod(renameVariables(a), renameVariables(left), renameVariables(right))
      case GetField.Z(o, f) =>
        GetField(renameVariables(o), f)
      case GetMethod.Z(o) =>
        GetMethod(renameVariables(o))
      case New.Z(bindings) =>
        New(bindings.mapValues(renameVariables).view.force)
      case Spawn.Z(c, t, b, comp) =>
        Spawn(renameVariables(c), renameVariables(t), b, renameVariables(comp))
      case NewTerminator.Z(t) =>
        NewTerminator(renameVariables(t))
      case Kill.Z(c, t, k) =>
        Kill(renameVariables(c), renameVariables(t), renameVariables(k))
      case CheckKilled.Z(t) =>
        CheckKilled(renameVariables(t))
      case TryOnException.Z(b, h) =>
        TryOnException(renameVariables(b), renameVariables(h))

      case NewSimpleCounter.Z(b, h) =>
        NewSimpleCounter(renameVariables(b), renameVariables(h))
      case NewServiceCounter.Z(c, c2, t) =>
        NewServiceCounter(renameVariables(c), renameVariables(c2), renameVariables(t))
      case NewTerminatorCounter.Z(c, t) =>
        NewTerminatorCounter(renameVariables(c), renameVariables(t))
      case HaltToken.Z(c) =>
        HaltToken(renameVariables(c))
      case NewToken.Z(c) =>
        NewToken(renameVariables(c))
      case SetDiscorporate.Z(c) =>
        SetDiscorporate(renameVariables(c))
      case TryFinally.Z(b, h) =>
        TryFinally(renameVariables(b), renameVariables(h))

      case Bind.Z(fut, comp) =>
        Bind(renameVariables(fut), renameVariables(comp))
      case BindStop.Z(fut) =>
        BindStop(renameVariables(fut))
      case f @ NewFuture.Z(raceFreeResolution) =>
        f.value
    })
  }

  val letInlineThreshold = co.options.optimizationFlags("porc:let-inline-threshold").asInt(30)
  val letInlineCodeExpansionThreshold = co.options.optimizationFlags("porc:let-inline-expansion-threshold").asInt(30)
  val referenceThreshold = co.options.optimizationFlags("porc:let-inline-ref-threshold").asInt(5)

  val InlineLet = OptFull("inline-let") { (expr, a) =>
    expr match {
      case Let.Z(x, lam @ Continuation.Z(formals, impl), scope) =>
        def size = Analysis.cost(impl.value)
        lazy val (noncompatReferences, compatReferences, compatCallsCost) = {
          var refs = 0
          var refsCompat = 0
          var callsCost = 0
          (new Transform {
            override def onArgument = {
              case Zipper(`x`, _) =>
                refs += 1
                x
            }
            override def onExpression = {
              case e @ CallContinuation.Z(Zipper(`x`, _), _) =>
                refsCompat += 1
                callsCost += Analysis.cost(e.value)
                e.value
              case a: Argument.Z if onArgument.isDefinedAt(a) =>
                onArgument(a)
            }
          })(scope)
          (refs - refsCompat, refsCompat, callsCost)
        }

        val codeExpansion = compatReferences * size - compatCallsCost -
          (if (noncompatReferences == 0) size else 0)

        def doInline(rename: Boolean) = new Transform {
          override def onExpression = {
            case CallContinuation.Z(Zipper(`x`, _), args) =>
              val res = impl.replace(impl.value.substAll((formals zip args.map(_.value)).toMap))
              if (rename)
                renameVariables(res)(Map[Variable, Variable]())
              else
                res.value
          }
        }

        //Logger.finer(s"Attempting inline: $x: $compatReferences $noncompatReferences $compatCallsCost $size; $codeExpansion")
        if (compatReferences > 0 && codeExpansion <= letInlineCodeExpansionThreshold) {
          if (noncompatReferences > 0)
            Some(Let(x, lam.value, doInline(true)(scope)))
          else
            Some(doInline(compatReferences != 1)(scope))
        } else {
          None
        }
      case Let.Z(x, Zipper(a: Argument, _), scope) =>
        Some(scope.value.substAll(Map((x, a))))
      case e =>
        None
    }
  }

  val TryCatchElim = Opt("try-catch-elim") {
    // TODO: Figure out why this is taking multiple passes to finish. This should eliminate all excess onHalted expressions in one pass.
    case (TryOnException.Z(Zipper(BindingSequence(bindings, TryOnException(b, h1)), _), h2), a) if h1 == h2.value =>
      TryOnException(BindingSequence(bindings, b), h2.value)
  }

  val TryFinallyElim = Opt("try-finally-elim") {
    // TODO: Figure out why this is taking multiple passes to finish. This should eliminate all excess onHalted expressions in one pass.
    case (TryFinally.Z(Zipper(BindingSequence(bindings, TryFinally(b, h1)), _), h2), a) if h1 == h2.value =>
      TryFinally(BindingSequence(bindings, b), h2.value)
  }

  val EtaReduce = Opt("eta-reduce") {
    case (Continuation.Z(formals, CallContinuation.Z(t, args)), a) if args.map(_.value) == formals =>
      t.value
    case (Continuation.Z(formals,
      TryOnException.Z(CallContinuation.Z(p, args), _)
      ), a) if args.map(_.value) == formals =>
      p.value
  }

  /** Transform this pattern:
    * ```
    * λ(v...).
    *   let comp = λ().
    *       P (v...) in
    *   try
    *     spawn_must C T comp_668
    *   catch
    *     ...
    * ```
    * into
    * ```
    * P
    * ```
    *
    */
  val EtaSpawnReduce = Opt("eta-spawn-reduce") {
    case (Continuation.Z(formals,
      Let.Z(comp, Continuation.Z(Seq(),
        CallContinuation.Z(p, args)
        ),
        TryOnException.Z(Spawn.Z(_, _, _, compRef), _)
        )
      ), a) if args.map(_.value) == formals && comp == compRef.value =>
      p.value
  }

  val VarLetElim = Opt("var-let-elim") {
    case (Let.Z(x, Zipper(y: Variable, _), b), a) => b.value.substAll(Map((x, y)))
  }

  val spawnCostInlineThreshold = co.options.optimizationFlags("porc:spawn-inline-threshold").asInt(-1)

  val InlineSpawn = OptFull("inline-spawn") { (e, a) =>
    e match {
      case Spawn.Z(c, t, blocking, e) => {
        def cost = Analysis.cost(e.value)
        def acceptableCost = spawnCostInlineThreshold < 0 || cost <= spawnCostInlineThreshold
        if (!blocking && acceptableCost)
          Some(CallContinuation(e.value, Seq()))
        else
          None
      }
      case _ => None
    }
  }
  
  def isTail(e: PorcAST.Z): Boolean = {
    val p = e.parent
    p map { p => p match {
        case Let.Z(_, _, `e`) => isTail(p)
        case Let.Z(_, `e`, _) => false
        case Continuation.Z(_, `e`) => isTail(p)
        case MethodDeclaration.Z(_, _, `e`) => isTail(p)
        case _: Argument.Z => isTail(p)
        case CallContinuation.Z(t, args) => false
        case Force.Z(p, c, t, futs) => false
        case Resolve.Z(p, c, t, futs) => false
        case Sequence.Z(exprs) if exprs.last == e => isTail(p)
        case Sequence.Z(_) => false
        case MethodCPSCall.Z(external, target, p, c, t, args) => false
        case MethodDirectCall.Z(external, target, args) => false
        case IfLenientMethod.Z(_, `e`, _) => isTail(p)
        case IfLenientMethod.Z(_, _, `e`) => isTail(p)
        case IfLenientMethod.Z(_, _, _) => false
        case GetField.Z(o, f) => false
        case GetMethod.Z(o) => false
        case New.Z(bindings) => false
        case Spawn.Z(c, t, b, comp) => false
        case NewTerminator.Z(t) => false
        case Kill.Z(c, t, k) => false
        case CheckKilled.Z(t) => false
        case TryOnException.Z(_, _) => isTail(p)
        case _: NewCounter.Z => false
        case HaltToken.Z(c) => false
        case NewToken.Z(c) => false
        case SetDiscorporate.Z(c) => false
        case TryFinally.Z(_, `e`) => isTail(p)
        case TryFinally.Z(_, _) => false
        case Bind.Z(fut, comp) => false
        case BindStop.Z(fut) => false
        case NewFuture.Z(_) => false
        case MethodCPS.Z(name, p, c, t, isDef, args, `e`) => true
        case MethodDirect.Z(name, isDef, args, `e`) => true
      }
    } getOrElse true
  }

  val TailSpawnElim = OptFull("tail-spawn-elim") { (e, a) =>
    e match {
      case Spawn.Z(_, _, _, k) if isTail(e) => {
        Some(CallContinuation(k.value, Seq()))
      }
      case _ => None
    }
  }


  val allOpts = List[Optimization](TailSpawnElim, InlineLet, EtaReduce, TryCatchElim, TryFinallyElim, EtaSpawnReduce, InlineSpawn)

  val opts = allOpts.filter { o =>
    val b = co.options.optimizationFlags(s"porc:${o.name}").asBool()
    Logger.fine(s"${if (b) "ENABLED" else "disabled"} ${o.name}")
    b
  }
}

object Optimizer {
  object :::> {
    def unapply(e: PorcAST.Z): Option[(Expression, Expression)] = e match {
      case Sequence.Z(e :: l) =>
        Some((e.value, Sequence(l.map(_.value))))
      case _ => None
    }
    def unapply(e: PorcAST): Option[(Expression, Expression)] = e match {
      case Sequence(e :: l) =>
        Some((e, Sequence(l)))
      case _ => None
    }
  }

  sealed abstract class BindingStatement {
  }

  object BindingStatement {
    case class MethodDeclaration(t: Argument, meths: Seq[Method]) extends BindingStatement
    case class Let(x: Variable, e: Expression) extends BindingStatement
    case class Statement(e: Expression) extends BindingStatement
  }

  object BindingSequence {
    def unapply(e: PorcAST): Some[(Seq[BindingStatement], PorcAST)] = e match {
      case Let(x, v, b) =>
        val BindingSequence(bindings, b1) = b
        Some((BindingStatement.Let(x, v) +: bindings, b1))
      case MethodDeclaration(t, meths, b) =>
        val BindingSequence(bindings, b1) = b
        Some((BindingStatement.MethodDeclaration(t, meths) +: bindings, b1))
      case s :::> PorcUnit() =>
        Some((Seq(), s))
      case s :::> ss =>
        val BindingSequence(bindings, b1) = ss
        //Logger.fine(s"unpacked sequence: $s $ss $bindings $b1")
        Some((BindingStatement.Statement(s) +: bindings, b1))
      case s =>
        Some((Seq(), s))
    }

    def apply(bindings: Seq[BindingStatement], b: Expression) = {
      bindings.foldRight(b)((bind, b) => {
        bind match {
          case BindingStatement.Let(x, v) =>
            Let(x, v, b)
          case BindingStatement.MethodDeclaration(t, meths) =>
            MethodDeclaration(t, meths, b)
          case BindingStatement.Statement(s) =>
            s ::: b
        }
      })
    }
  }
}

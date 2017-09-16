//
// Optimizer.scala -- Scala Optimizer
// Project OrcScala
//
// Created by amp on Sept 16, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.orctimizer

import orc.compile.Logger
import orc.values.OrcRecord
import orc.ast.orctimizer.named._
import orc.values.Field
import orc.lib.builtin.structured.TupleConstructor
import orc.lib.builtin.structured.TupleArityChecker
import orc.compile.CompilerOptions
import orc.types
import orc.values.sites.Site
import orc.lib.state.NewFlag
import orc.lib.state.SetFlag
import orc.lib.state.PublishIfNotSet
import orc.values.Signal
import orc.ast.hasAutomaticVariableName
import orc.error.compiletime.UnboundVariableException
import scala.collection.mutable
import orc.compile.OptimizerStatistics
import orc.compile.NamedOptimization
import orc.compile.AnalysisCache
import orc.compile.orctimizer.FlowGraph._
import orc.compile.orctimizer.CallGraphValues._
import orc.compile.orctimizer.DelayAnalysis.DelayInfo
import swivel.Zipper
import orc.compile.orctimizer.FlowGraph.EverywhereNode
import orc.ast.orctimizer.named.DeclareMethods
import orc.lib.builtin.structured.TupleArityChecker
import orc.lib.builtin.structured.TupleConstructor
import orc.lib.builtin.structured.TupleArityChecker

class HashFirstEquality[T](val value: T) {
  override def toString() = value.toString()
  override def hashCode() = value.hashCode()
  override def equals(o: Any) = o match {
    case o: HashFirstEquality[T] =>
      value.hashCode() == o.value.hashCode() && value == o.value
    case _ =>
      false
  }
}

object HashFirstEquality {
  def apply[T](v: T) = {
    new HashFirstEquality(v)
  }
}

class AnalysisResults(cache: AnalysisCache, e: Expression.Z) {
  lazy val callgraph: CallGraph = cache.get(CallGraph)((e, None))
  lazy val publications: PublicationCountAnalysis = cache.get(PublicationCountAnalysis)((e, None))
  lazy val effectsAnalysis: EffectAnalysis = cache.get(EffectAnalysis)((e, None))
  lazy val delays: DelayAnalysis = cache.get(DelayAnalysis)((e, None))
  lazy val forces: ForceAnalysis = cache.get(ForceAnalysis)((e, None))
  lazy val alreadyForced: AlreadyForcedAnalysis = cache.get(AlreadyForcedAnalysis)((e, None))

  private val exprMapping = mutable.HashMap[HashFirstEquality[Expression.Z], Expression.Z]()
  private val varMapping = mutable.HashMap[ValueNode, ValueNode]()

  // TODO: The mapping stuff seems to be a really large performance cost. Maybe if I make it a reference equality it will be better? But that would also be fragile.

  def addMapping(req: Argument.Z, real: Argument.Z): Unit = {
    varMapping += ((ValueNode(req), remap(ValueNode(real))))
  }
  def addMapping(req: Expression.Z, real: Expression.Z): Unit = {
    exprMapping += HashFirstEquality(req) -> remap(real)
  }

  private def remap(req: ValueNode) = varMapping.get(req).getOrElse(req)
  private def remap(req: Expression.Z) = exprMapping.get(HashFirstEquality(req)).getOrElse(req)

  def valuesOf(e: Argument.Z) = callgraph.valuesOf(remap(ValueNode(e)))
  def valuesOf(e: Expression.Z) = callgraph.valuesOf(remap(e))

  def publicationsOf(e: Expression.Z) = publications.publicationsOf(remap(e))

  def effects(e: Expression.Z): Boolean = {
    effectsAnalysis.effects(remap(e))
  }
  def effected(e: Expression.Z): Boolean = {
    effectsAnalysis.effected(remap(e))
  }

  def delayOf(e: Expression.Z): DelayInfo = {
    delays.delayOf(remap(e))
  }

  def nMappings = exprMapping.size + varMapping.size
}

trait Optimization extends ((Expression.Z, AnalysisResults) => Option[Expression]) with NamedOptimization {
  val name: String

  override def toString = name
}

case class Opt(name: String)(f: PartialFunction[(Expression.Z, AnalysisResults), Expression]) extends Optimization {
  def apply(e: Expression.Z, cache: AnalysisResults): Option[Expression] = f.lift((e, cache))
}
case class OptSimple(name: String)(f: PartialFunction[Expression.Z, Expression]) extends Optimization {
  def apply(e: Expression.Z, cache: AnalysisResults): Option[Expression] = f.lift(e)
}
case class OptFull(name: String)(f: (Expression.Z, AnalysisResults) => Option[Expression]) extends Optimization {
  def apply(e: Expression.Z, cache: AnalysisResults): Option[Expression] = f(e, cache)
}

// TODO: Implement compile time evaluation of select sites.

/* Assumptions in the optimizer:
 *
 * No call (def or site) can publish a future.
 *
 */

/** @author amp
  */
abstract class Optimizer(co: CompilerOptions) extends OptimizerStatistics {
  def opts: Seq[Optimization]
  
  val optimizeOptimizationResult = false

  def apply(e: Expression.Z, cache: AnalysisCache): Expression = {
    val optimizationTransform: Transform = new Transform { optimizationTransform =>
      val results = new AnalysisResults(cache, e)
      
      override val onExpression = {
        case (e: Expression.Z) => {
          import orc.util.StringExtension._
          //Logger.finer(s"Optimizing:${" " * e.parents.size}${e.value.getClass.getSimpleName} ${e.value.toString.truncateTo(40)}")
          val e1 = opts.foldLeft(e)((e, opt) => {
            //Logger.finer(s"invoking ${opt.name} on:\n${e.value.toString.truncateTo(120)}")
            opt(e, results) match {
              case None => e
              case Some(e2) =>
                val e3 = if (e.value != e2) {
                  Logger.finer(s"${opt.name}:\n${e.value.toString.truncateTo(120)}\n====>\n${e2.toString.truncateTo(120)}")
                  countOptimization(opt)
                  val e3 = e.replace(e.value ->> e2)
                  results.addMapping(e3, e)
                  e3
                } else {
                  e
                }
                e3
            }
          })
          if(optimizeOptimizationResult && e1 != e)
            optimizationTransform(e1)
          else
            e1.value
        }
      }
    }
  
    val r = optimizationTransform(e)

    r
  }

  /*
  Analysis needed:

  Information about forcing of expressions. What it forces, and if it halts with it. What is forces may also need to be categorized into before and after first side-effect/publication.

  */

  case object FoundException extends RuntimeException

  val FutureElim = OptFull("future-elim") { (e, a) =>
    def isFutureEliminable(future: NamedAST.Z, body: Expression.Z): Boolean = {
      def byNonBlocking1 = a.delayOf(body).maxFirstPubDelay == ComputationDelay() && (a.publicationsOf(body) only 1)
      
      def surroundingFutureIsForced = {
        val surroundingFuture = future.parents.tail.collectFirst({ case FieldFuture.Z(b) => b; case Future.Z(b) => b })        
        surroundingFuture match {
          case None => false
          case Some(surroundingF) =>
            val alreadyForced = a.alreadyForced(body).collect({ case n: WithSpecificAST => n.location })
            alreadyForced contains surroundingF
        }
      }
      
      val sequentialize = AnnotationHack.inAnnotation[Sequentialize](body)
      
      sequentialize || (byNonBlocking1 && !surroundingFutureIsForced)
    }
    
    e match {
      case Future.Z(body) => {
        if (isFutureEliminable(e, body))
          Some(body.value)
        else
          None
      }
      case n @ New.Z(self, _, fields, _) => {
        var changed = false
        val newFields = fields.mapValues({
          case f @ FieldFuture.Z(a: Argument.Z) if !a.freeVars.contains(self) => {
            changed = true
            (FieldArgument(a.value), None, None)
          }
          case f @ FieldFuture.Z(body) if !body.freeVars.contains(self) => {
            if (isFutureEliminable(f, body)) {
              changed = true
              val x = new BoundVar(Some(hasAutomaticVariableName.getNextVariableName("fieldVal")))
              (FieldArgument(x), Some(body), Some(x))
            } else
              (f.value, None, None)
          }
          case f => (f.value, None, None)
        }).view.force

        if (changed) {
          val exprs = newFields.values.collect({ case (_, Some(expr), Some(x)) => (expr, x) })
          val newF = newFields.mapValues(_._1).view.force
          Some(exprs.foldRight(n.value.copy(bindings = newF): Expression)((p, c) => Branch(p._1.value, p._2, c)))
        } else
          None
      }
      case _ => None
    }
  }

  val UnusedFutureElim = Opt("unused-future-elim") {
    case (Branch.Z(Future.Z(f), x, g), a) if !(g.freeVars contains x) =>
      (f.value >> Stop()) || g.value
  }

  val GetMethodElim = Opt("getmethod-elim") {
    case (GetMethod.Z(o), a) if a.valuesOf(o).forall({
      case NodeValue(ExitNode(Call.Z(Constant.Z(TupleConstructor), _, _))) => true
      case NodeValue(ExitNode(Call.Z(Constant.Z(TupleArityChecker), _, _))) => true
      case n: NodeValue[_] => n.isMethod
      case _ => false
    }) =>
      o.value
    case (e @ GetMethod.Z(o), a) if e.parents.collectFirst({ case Branch.Z(GetMethod.Z(Zipper(`o`, _)), _, _) => true }).isDefined =>
      e.parents.collectFirst({ case Branch.Z(GetMethod.Z(Zipper(`o`, _)), x, _) => x }).get
  }

  val FutureForceElim = OptFull("future-force-elim") { (e, a) =>
    e match {
      case Branch.Z(Future.Z(e), x, f) =>
        val fforces = a.forces(f)
        val publications = a.publicationsOf(e)
        if(fforces.contains(x) && publications <= 1)
          Some(Branch(e.value, x, f.value))
        else {
          //Logger.info(s"Failed to apply future-force-elim $fforces $publications\nfuture { ${e.value.toString.take(100)} }\n>$x>\n${f.value.toString.take(100)}")
          None
        }
      case Branch.Z(e, x, f) => 
        //Logger.info(s"Failed to apply future-force-elim\n${e.value}\n>$x>\n${f.value}")
        None
      case _ => 
        None
    }
  }

  val ForceElim = OptFull("force-elim") { (e, a) =>
    import orc.compile.orctimizer.CallGraphValues._
    
    e match {
      case Force.Z(xs, vs, body) => {
        def eliminateNonFutures() = {
          val (fs, nfs) = (xs zip vs).partition(v => a.valuesOf(v._2).futures.nonEmpty)
          
          val (newXs, newVs) = fs.unzip
          val newBody = body.value.substAll(nfs.map(p => (p._1, p._2.value)).toMap[Argument, Argument])
          if (nfs.isEmpty)
            None
          else if (fs.nonEmpty)
            Some(Force(newXs, newVs.map(_.value), newBody))
          else
            Some(newBody)
        }
        
        def eliminateDuplicateForces() = {
          def findParentForce(v: Argument) = {
            def isV(w: Argument.Z) = w.value == v
            val parents = e.parents.tail
            parents.collectFirst({ 
              case Force.Z(xs, vs, _) if vs.exists(isV) => xs(vs.indexWhere(isV)) 
            })
          }
  
          val allInformation = (xs, vs.view.force, vs.map(v => findParentForce(v.value)).view.force).zipped
          val nfs = allInformation.collect({ 
            case (x, _, Some(y)) => (x, y)
          }).toMap[Argument, Argument]
          val fs = allInformation.collect({ 
            case (x, v, None) => (x, v.value)
          }).toSeq
          
          val (newXs, newVs) = fs.unzip
          val newBody = body.value.substAll(nfs)
          if (nfs.isEmpty)
            None
          else if (fs.nonEmpty)
            Some(Force(newXs, newVs, newBody))
          else
            Some(newBody)
        }
        eliminateNonFutures() orElse eliminateDuplicateForces()
      }
      case _ => None
    }
  }

  val ResolveElim = OptFull("resolve-elim") { (e, a) =>
    e match {
      case Resolve.Z(vs, body) => {
        val (fs, nfs) = vs.partition(v => a.valuesOf(v).futures.nonEmpty)
        if (fs.isEmpty)
          Some(body.value)
        else
          None
      }
      case _ => None
    }
  }

  val IfDefElim = OptFull("ifdef-elim") { (e, a) =>
    import orc.compile.orctimizer.CallGraphValues._
    e match {
      // TODO: The f == g should actually be equivalence up to local variable renaming.
      case IfLenientMethod.Z(v, f, g) if f.value == g.value =>
        Some(f.value)
      case IfLenientMethod.Z(v, f, g) =>
        val vs = CallGraph.targetsFromValue(a.valuesOf(v)).toSet
        val hasLenient = vs.exists(_ match {
          case NodeValue(MethodNode(_: Routine.Z, _)) => true
          case NodeValue(ExitNode(_: Call.Z)) => true
          case NodeValue(EverywhereNode) => true
          case _ => false
        })
        val hasStrict = vs.exists(_ match {
          case NodeValue(MethodNode(_: Service.Z, _)) => true
          case n: NodeValue[_] if !n.isExternalMethod.isFalse => true
          case NodeValue(ExitNode(_: Call.Z)) => true
          case NodeValue(EverywhereNode) => true
          case _ => false
        })
        if (hasLenient && !hasStrict) {
          Some(f.value)
        } else if (!hasLenient && hasStrict) {
          Some(g.value)
        } else {
          // If either both are available or neither then just leave this as is.
          None
        }
      case _ => None
    }
  }

  val StopEquiv = Opt("stop-equiv") {
    case (f, a) if f.value != Stop() &&
      (a.publicationsOf(f) only 0) &&
      (!a.effects(f)) &&
      a.delayOf(f).maxHaltDelay == ComputationDelay() =>
      //Logger.info(s"stop-equiv: ${f.value}\n====\n${a.delayOf(f)} ${a.publicationsOf(f)} ${a.effects(f)}")
      Stop()
  }

  val StopElim = OptSimple("stop-elim") {
    case Parallel.Z(Stop.Z(), g) => g.value
    case Parallel.Z(f, Stop.Z()) => f.value
    case Otherwise.Z(Stop.Z(), g) => g.value
    case Otherwise.Z(f, Stop.Z()) => f.value
    case Branch.Z(Stop.Z(), _, g) => Stop()
  }

  val BranchElimArg = OptFull("branch-elim-arg") { (e, a) =>
    e match {
      case Branch.Z(f, x, y) if x == y.value =>
        Some(f.value)
      case _ => None
    }
  }

  val Normalize = OptFull("normalize") { (e, a) =>
    e match {
      case Branch.Z(f, x, y) =>
        Some(Branch(f.value, x, y.value))
      case Otherwise.Z(f, y) =>
        Some(Otherwise(f.value, y.value))
      case _ => None
    }
  }

  val BranchElimConstant = OptFull("branch-elim-const") { (e, a) =>
    import orc.compile.orctimizer.CallGraphValues._
    e match {
      case Branch.Z(a: Argument.Z, x, y) =>
        Some(y.value.subst(a.value, x))
      case Branch.Z(c, x, y) if (a.publicationsOf(c) only 1) && !a.effects(c) =>
        val vs = a.valuesOf(c).toSet
        val DelayInfo(delay, _) = a.delayOf(c)
        if (vs.size == 1 && delay == ComputationDelay()) {
          vs.head match {
            case NodeValue(ConstantNode(Constant(v), _)) =>
              Some(y.value.subst(Constant(v), x))
            case NodeValue(MethodNode(Method.Z(name, _, _, _, _, _), _)) if (e.freeVars contains name) && (e.contextBoundVars contains name) =>
              Some(y.value.subst(name, x))
            case _ =>
              None
          }
        } else {
          None
        }
      case _ =>
        None
    }
  }

  val BranchElim = OptFull("branch-elim") { (e, a) =>
    e match {
      case Branch.Z(f, x, g) if a.publicationsOf(f) only 0 => Some(f.value)
      case Branch.Z(f, x, g) if !a.effects(f) &&
        a.delayOf(f).maxFirstPubDelay == ComputationDelay() && a.delayOf(f).maxHaltDelay == ComputationDelay() &&
        (a.publicationsOf(f) only 1) && !g.freeVars.contains(x) => Some(g.value)
      case Branch.Z(f, x, g) if !a.effects(f) =>
        val valueExpr = f.value
        val parents = e.parents.tail
        parents.collectFirst({ case Branch.Z(Zipper(`valueExpr`, _), y, _) => y }) map { y =>
          g.value.subst(y, x)
        }
      case _ => None
    }
  }
  
  val OtherwiseElim = OptFull("otherwise-elim") { (e, a) =>
    e match {
      case Otherwise.Z(f, g) if a.publicationsOf(f) > 0 => Some(f.value)
      case Otherwise.Z(f, g) if !a.effects(f) && (a.publicationsOf(f) only 0) && 
        a.delayOf(f).maxHaltDelay == ComputationDelay() => Some(g.value)
      case _ => None
    }
  }

  val TrimElim = Opt("trim-elim") {
    case _ if false => ???
    // FIXME: Reenable this when DelayAnalysis is fixed for recursive functions.
    //case (Trim.Z(f), a) if a.publicationsOf(f) <= 1 && a.delayOf(f).maxHaltDelay == ComputationDelay() && !a.effects(f) => f.value
  }
  
  var nextInlineNumberCounter = 0
  
  def nextInlineNumber(): Int = {
    nextInlineNumberCounter += 1
    nextInlineNumberCounter
  }
  
  def newName(x: BoundVar) = {
    new BoundVar(x.optionalName.map(_ + s"i${nextInlineNumber()}"))
  }
  def newName(x: BoundTypevar) = {
    new BoundTypevar(x.optionalName.map(_ + s"i${nextInlineNumber()}"))
  }

  def renameVariables(newN: BoundVar, e: Method.Z)(implicit mapping: Map[BoundVar, Argument], tmapping: Map[BoundTypevar, Type]): Method = {
    e.value ->> (e match {
      case m@Method.Z(name, args, b, targs, argTypes, returnType) =>
        val newArgs = args.map(newName)
        val newTArgs = targs.map(newName)
        val newMapping = mapping ++ (args zip newArgs)
        val newTMapping = tmapping ++ (targs zip newTArgs)
        m.value.copy(newN, newArgs, renameVariables(b)(newMapping, newTMapping),
            newTArgs, argTypes.map(_.map(renameVariables(_)(newMapping, newTMapping))), returnType.map(renameVariables(_)(newMapping, newTMapping)))
    })
  }

  def renameVariables(e: FieldValue.Z)(implicit mapping: Map[BoundVar, Argument], tmapping: Map[BoundTypevar, Type]): FieldValue = {
    e.value ->> (e match {
      case FieldArgument.Z(a) => FieldArgument(renameVariables(a))
      case FieldFuture.Z(e) => FieldFuture(renameVariables(e))
    })
  }

  def renameVariables(e: Argument.Z)(implicit mapping: Map[BoundVar, Argument], tmapping: Map[BoundTypevar, Type]): Argument = {
    e.value ->> (e.value match {
      case v: BoundVar =>
        mapping.getOrElse(v, v)
      case a =>
        a
    })
  }

  def renameVariables(e: Type.Z)(implicit mapping: Map[BoundVar, Argument], tmapping: Map[BoundTypevar, Type]): Type = {
    val typeRewrite = new Transform {
      override val onType = {
        case v: BoundTypevar.Z =>
          tmapping.getOrElse(v.value, v.value)
      }
    }
    typeRewrite(e)
  }

  def renameVariables(e: Expression.Z)(implicit mapping: Map[BoundVar, Argument], tmapping: Map[BoundTypevar, Type]): Expression = {
    e.value ->> (e match {
      case Stop.Z() => e.value
      case Future.Z(e) => Future(renameVariables(e))
      case Force.Z(xs, es, b) => {
        val newXs = xs.map(newName).view.force
        Force(newXs, es.map(renameVariables).view.force, renameVariables(b)(mapping ++ (xs zip newXs), tmapping))
      }
      case Resolve.Z(es, b) => Resolve(es.map(renameVariables).view.force, renameVariables(b))
      case Call.Z(target, args, targs) => {
        Call(renameVariables(target), args.map(renameVariables).view.force, targs.map(_.map(renameVariables)))
      }
      case Parallel.Z(l, r) => Parallel(renameVariables(l), renameVariables(r))
      case Branch.Z(l, x, r) => {
        val newX = newName(x)
        Branch(renameVariables(l), newX, renameVariables(r)(mapping + ((x, newX)), tmapping))
      }
      case Trim.Z(e) => Trim(renameVariables(e))
      case Otherwise.Z(l, r) => Otherwise(renameVariables(l), renameVariables(r))
      case DeclareMethods.Z(methods, body) => {
        val newMethodNames = methods.map(m => newName(m.name)).view.force
        val newMapping = mapping ++ (methods.map(_.name) zip newMethodNames)
        val newMethods = (newMethodNames zip methods).map(p => renameVariables(p._1, p._2)(newMapping, tmapping)).view.force
        DeclareMethods(newMethods, renameVariables(body)(newMapping, tmapping))
      }
      case GetMethod.Z(e) => GetMethod(renameVariables(e))
      case GetField.Z(e, f) => GetField(renameVariables(e), f)
      case IfLenientMethod.Z(c, e, f) => IfLenientMethod(renameVariables(c), renameVariables(e), renameVariables(f))
      case New.Z(self, selfType, bindings, objType) => {
        val newSelf = newName(self)
        val newMapping = mapping + ((self, newSelf))
        val newBindings = bindings.mapValues(b => renameVariables(b)(newMapping, tmapping)).view.force
        New(newSelf, selfType.map(renameVariables(_)(newMapping, tmapping)), newBindings, objType.map(renameVariables(_)(newMapping, tmapping)))
      }
      case DeclareType.Z(n, t, b) => {
        val newN = newName(n)
        val newTMapping = tmapping + ((n, newN))
        DeclareType(newN, renameVariables(t)(mapping, newTMapping), renameVariables(b)(mapping, newTMapping))
      }
      case HasType.Z(e, t) => HasType(renameVariables(e), renameVariables(t))
      case a: Argument.Z => renameVariables(a)
    })
  }

  def inliningCost(e: Expression.Z): Int = {
    var n = 0
    val countExpressions = new Transform {
      override val onExpression = {
        case e: Argument.Z => e.value 
        case e => n += 1; e.value
      }
    }
    countExpressions(e)
    n
  }
  
  val inlineCostThreshold = co.options.optimizationFlags("orct:inline-threshold").asInt(15)
  val higherOrderInlineCostThreshold = co.options.optimizationFlags("orct:higher-order-inline-threshold").asInt(200)

  val Inline = OptFull("inline") { (e, a) =>
    e match {
      case Call.Z(target, args, targs) => {
        val vs = a.valuesOf(target).toSet
        if (vs.size == 1) {
          vs.head match {
            case NodeValue(MethodNode(method @ Routine.Z(name, formals, body, tformals, _, _), _)) if formals.size == args.size =>
              lazy val directCallCount = {
                val Some(DeclareMethods.Z(_, scope)) = method.parent
                var n = 0
                val countCalls = new Transform {
                  override val onArgument = {
                    case Zipper(`name`, _) => n += 1000; name
                  }
                  override val onExpression = {
                    case Call.Z(Zipper(`name`, _), _, _) => n += 1; e.value 
                    case a: Argument.Z if onArgument.isDefinedAt(a) => onArgument(a)
                  }
                }
                countCalls(scope)
                n
              }
              
              lazy val hasCapturedVariables = {
                (body.freeVars -- formals).nonEmpty 
              }
              lazy val isRecursive1 = e.parents.exists {
                case Routine.Z(`name`, _, _, _, _, _) => true
                case _ => false
              }
              lazy val isRecursive2 = {
                val searchForRecursiveCall = new Transform {
                  override val onExpression = {
                    case e@Call.Z(target, _, _) => {
                      a.valuesOf(target).exists {
                        case NodeValue(MethodNode(Routine.Z(`name`, _, _, _, _, _), _)) => throw FoundException
                        case _ => false
                      }
                      e.value
                    }
                  }
                }
                try {
                  searchForRecursiveCall(body)
                  false
                } catch {
                  case FoundException =>
                    true
                }
              }
              def isRecursive = isRecursive1 || isRecursive2
              lazy val cost = inliningCost(body)
              lazy val hasClosureArgument = args.exists { arg =>
                def isMethod(e: Value[_]) = e match {
                  case NodeValue(MethodNode(_, _)) => true
                  case _ => false
                }
                a.valuesOf(arg).exists(isMethod)
              }
              
              //Logger.finer(s"Attempting inline of $name at ${e.value}: $isRecursive1 $isRecursive2 $cost $hasClosureArgument $hasCapturedVariables")
              if (isRecursive || (target != name && hasCapturedVariables)) {
                // Never inline recursive functions or functions with captured variables where we are not referencing the function directly.
                None
              } else if (directCallCount <= 1 || cost < inlineCostThreshold || (cost < higherOrderInlineCostThreshold && hasClosureArgument)) {
                Some(renameVariables(body)(Map() ++ (formals zip args.map(_.value)), 
                    Map() ++ (tformals zip targs.getOrElse(Seq()).map(_.value))))
              } else {
                None
              }
            case _ =>
              None
          }
        } else {
          None
        }
      }
      case _ => None
    }
  }
  
  val MethodElim = Opt("method-elim") {
    case (DeclareMethods.Z(methods, b), a) if (b.freeVars & methods.map(_.name).toSet).isEmpty => b.value
  }

  case object OptimizationNotApplicableException extends RuntimeException
  def abortOptimization() = throw OptimizationNotApplicableException
  def attemptOptimization[T](f: => Option[T]): Option[T] = try { f } catch { case OptimizationNotApplicableException => None }
  
  val TupleElim = OptFull("tuple-elim") { (e, a) =>
    // We use exceptions for flow control (see abortOptimization and attemptOptimization). Think in ML-style and you should feel OK about it.
    e match {
      case Call.Z(Constant.Z(TupleArityChecker), Seq(tupleArg, sizeArg), _) =>
        attemptOptimization {
          //Logger.finer(s"TupleArityChecker(${tupleArg.value}, ${sizeArg.value})")
          val sizeVs = a.valuesOf(sizeArg).toSet.map({
            case v @ NodeValue(_) => v.constantValue match {
              case Some(n: Number) => n.intValue
              case _ =>
                //Logger.finer(s"Tuple arity: failed due to size node value $v")
                abortOptimization()
            }
            case v => 
              //Logger.finer(s"Tuple arity: failed due to size value $v")
              abortOptimization()
          })
          if (sizeVs.size != 1) abortOptimization()
          val size = sizeVs.head
          
          //Logger.finer(s"TupleArityChecker(${tupleArg.value}, $size)")
          
          val tupleSizes = a.valuesOf(tupleArg).toSet.map({
            case NodeValue(ExitNode(Call.Z(Constant.Z(TupleConstructor), elements, _))) => 
              elements.size
            case _ => abortOptimization()
          })

          //Logger.finer(s"TupleArityChecker($tupleSizes, $size)")

          if (tupleSizes == Set(size)) {
            Some(tupleArg.value)
          } else {
            None
          }
        }
      case Call.Z(targetArg, Seq(indexArg), _) =>
        attemptOptimization {
          val indexVs = a.valuesOf(indexArg).toSet.map({
            case v @ NodeValue(_) => v.constantValue match {
              case Some(n: Number) => n.intValue
              case _ => abortOptimization()
            }
            case _ => abortOptimization()
          })
          if (indexVs.size != 1) abortOptimization()
          val index = indexVs.head
          
          //Logger.finer(s"Tuple get ${e.value}: target = ${a.valuesOf(targetArg)} index = $index")

          val values = a.valuesOf(targetArg).toSet.map({
            case NodeValue(ExitNode(Call.Z(Constant.Z(TupleConstructor), elements, _))) if elements.size > index => 
              elements(index)
            case v =>
              //Logger.finer(s"Tuple get ${e.value}: failed due to tuple value $v")
              abortOptimization()
          })
          //Logger.finer(s"Tuple get ${e.value}: values = ${values.map(_.value)}")
          
          if (values.size != 1) abortOptimization()
          val value = values.head

          val valueConstants = a.valuesOf(value).toSet.map({
            case v @ NodeValue(_) => v.constantValue
            case _ => None
          })
          if (valueConstants.size == 1 && valueConstants.head.isDefined) {
            // Just propagate the constant if that's what was in the tuple
            Some(Constant(valueConstants.head.get))
          } else if (e.contextBoundVars contains value.value) {
            // If we are in scope of the binding used in the tuple put in the variable name.
            Some(value.value)
          } else {
            None
          }
        }
      case _ => None
    }
  }
  
  /*
  val LiftForce = OptFull("lift-force") { (e, a) =>
    import a.ImplicitResults._
    val freevars = e.freeVars
    val vars = e.forceTypes.flatMap {
      case (v, t) if freevars.contains(v) && t <= ForceType.Eventually(false) => Some(v)
      case _ => None
    }
    e match {
      case Pars(es, ctx) if es.size > 1 && vars.size > 0 => {
        val forceLimit = ForceType.Immediately(true)
        val (bestVar, matchingEs) = vars.map({ v =>
          val alreadyLifted = ctx.bindings exists {
            // TODO: The full force requirement may be too strong
            case b @ Bindings.ForceBound(_, _, _) if b.publishForce => true
            case _ => false
          }
          if (alreadyLifted || (v in ctx).valueForceDelay == Delay.NonBlocking) (v, 0)
          else (v, es.count { e => (e in ctx).forces(v) <= forceLimit })
        }).maxBy(_._2)

        if (matchingEs <= 1) {
          None
        } else {
          // We know at this point that there will be at least 2 matching elements.
          // But I leave checks for clarity.
          val (forcers, nonforcers) = es.partition(e => (e in ctx).forces(bestVar) <= forceLimit)

          // TODO: Allow lifting non-closure-forcing forces.
          def processedForcers = {
            val y = new BoundVar()
            val trans = new ContextualTransform.NonDescending {
              override def onExpressionCtx = {
                case ForceAt(xs, vs, true, e) if vs.exists(_.e == bestVar) =>
                  val revMap = (vs.map(_.e) zip xs).toMap
                  val (newVs, newXs) = (revMap - bestVar).unzip
                  val newE = e.subst(y, revMap(bestVar))
                  if (newXs.size > 0)
                    Force(newXs.toList, newVs.toList, true, newE)
                  else
                    newE
              }
            }
            Force(y, bestVar, true, trans(Pars(forcers)))
          }
          (forcers.size, nonforcers.isEmpty) match {
            case (n, _) if n <= 1 => None
            case (_, false) => Some(processedForcers || Pars(nonforcers))
            case (_, true) => Some(processedForcers)
          }
        }
      }
      case _ => None
    }
  }

  val BranchExp = Opt("branch-expansion") {
    case (e @ (Pars(fs, ctx) > x > g), a) if fs.size > 1 && fs.exists(f => a(f in ctx).silent) => {
      // This doesn't really eliminate any code and I cannot think of a case where
      val (sil, nsil) = fs.partition(f => a(f in ctx).silent)
      (sil.isEmpty, nsil.isEmpty) match {
        case (false, false) => (Pars(nsil) > x > g) || Pars(sil)
        case (false, true) => Pars(sil)
        case (true, false) => e
      }
    }
    case (e @ (Pars(fs, ctx) > x > g), a) if fs.size > 1 && fs.exists(f => f.isInstanceOf[Constant]) => {
      val cs = fs.collect { case c: Constant => c }
      val es = fs.filter(f => !f.isInstanceOf[Constant])
      (cs.isEmpty, es.isEmpty) match {
        case (false, false) => (Pars(es) > x > g.e) || Pars(cs.map(g.e.subst(_, x)))
        case (false, true) => Pars(cs.map(g.e.subst(_, x)))
        case (true, false) => e.e
      }
    }
  }


  val LiftUnrelated = Opt("lift-unrelated") {
    case (e @ (g > x > Pars(es, ctx)), a) if a(g).nonBlockingPublish && (a(g).publications only 1) &&
      es.exists(e => !e.freeVars.contains(x)) => {
      val (f, h) = es.partition(e => e.freeVars.contains(x))
      (f.isEmpty, h.isEmpty) match {
        case (false, false) => (g > x > Pars(f)) || Pars(h)
        case (true, false) => (g >> Stop()) || Pars(h)
        case (false, true) => e
      }
    }
  }

  val TrimCompChoice = Opt("trim-compiler-choice") {
    case (TrimAt(Pars(fs, ctx)), a) if fs.size > 1 && fs.exists(f => a(f in ctx).nonBlockingPublish) => {
      // This could even be smarter and pick the "best" or "fastest" expression.
      val Some(f1) = fs.find(f => a(f in ctx).nonBlockingPublish)
      Trim(f1)
    }
  }

  val unrollCostThreshold = co.options.optimizationFlags("orct:unroll-threshold").asInt(45)

  val UnrollDef = OptFull("unroll-def") { (e, a) =>
    import a.ImplicitResults._

    throw new AssertionError("Unrolling doesn't work.")

    e match {
      case CallDefAt((f: BoundVar) in ctx, args, targs, _) => ctx(f) match {
        // TODO: Add unrolling for sites.
        case Bindings.RecursiveCallableBound(dctx, decls, d: Def) => {
          val DeclareCallablesAt(_, declsctx, _) = decls in dctx
          val DefAt(_, _, body, _, _, _, _) = d in declsctx
          def cost = Analysis.cost(body)
          if (cost > unrollCostThreshold) {
            Logger.finest(s"Failed to unroll: ${e.e} cost=$cost (unrollCostThreshold=$unrollCostThreshold)")
            None
          } else {
            Some(buildInlineDef(d, args, targs, declsctx, a))
          }
        }
        case _ => None
      }
      case _ => None
    }
  }

  def containsType(b: Expression, tv: BoundTypevar) = {
    var found = false
    (new NamedASTTransform {
      override def onType(typecontext: List[BoundTypevar]) = {
        case `tv` =>
          found = true; tv
        case e: Typevar => e
      }
    })(b)
    found
  }
  val TypeElim = Opt("type-elim") {
    case (DeclareTypeAt(tv, t, b), a) if !containsType(b, tv) => b
  }
  */
}

case class StandardOptimizer(co: CompilerOptions) extends Optimizer(co) {
  val allOpts = List(
      Normalize,
      IfDefElim, Inline, FutureForceElim, BranchElim, OtherwiseElim, TrimElim, UnusedFutureElim, StopEquiv, 
      ForceElim, ResolveElim, BranchElimArg, StopElim, BranchElimConstant, 
      FutureElim, GetMethodElim, TupleElim, MethodElim)
  /*
  val allOpts = List(
    BranchReassoc,
    DefBranchNorm, DefElim,
    LiftUnrelated, LiftForce,
    FutureElimFlatten, /*UnusedFutureElim,*/ FutureElim,
    /*FutureForceElim,*/ ForceElim, IfDefElim,
    TupleElim, AccessorElim,
    TrimCompChoice, TrimElim, ConstProp,
    StopEquiv, StopElim,
    BranchExp, BranchElim, BranchElimVar,
    InlineDef, TypeElim)
    */

  val opts = allOpts.filter { o =>
    val b = co.options.optimizationFlags(s"orct:${o.name}").asBool()
    Logger.fine(s"${if (b) "ENABLED" else "disabled"} ${o.name}")
    b
  }
}
/*
case class UnrollOptimizer(co: CompilerOptions) extends Optimizer(co) {
  val allOpts = List(
    UnrollDef)

  val opts = allOpts

  def transformFrom(f: PartialFunction[WithContext[Expression], Expression]) = new ContextualTransform.Post {
    override def onExpressionCtx = f
  }
}
*/

object Optimizer {
  //import WithContext._

  /*
  object Pars {
    private def pars(p: Expression): List[Expression] = {
      p match {
        case f || g => pars(f) ++ pars(g)
        case e => List(e)
      }
    }
    def unapply(e: Expression): Option[List[Expression]] = Some(pars(e))
    def unapply(e: WithContext[Expression]): Option[(List[Expression], TransformContext)] = {
      Some(pars(e.e), e.ctx)
    }

    def apply(l: Traversable[Expression]) = l.reduce(_ || _)
  }

  /** Match a sequence of expressions in the form: e1 >x1> ... >xn-1> en (ignoring association)
    */
  object Seqs {
    private def seqsAt(p: WithContext[Expression]): (List[(WithContext[Expression], BoundVar)], WithContext[Expression]) = {
      p match {
        case f > x > g => {
          val (fss, fn) = seqsAt(f) // f1 >x1> ... >xn-1> fn
          val (gss, gn) = seqsAt(g) // g1 >y1> ... >yn-1> gn
          // TODO: Add an assert here to check that no variables are shadowed. This should never happen because of how variables are handled and allocated.
          // f1 >x1> ... >xn-1> fn >x> g1 >y1> ... >yn-1> gn
          (fss ::: (fn, x) :: gss, gn)
        }
        case e => (Nil, e)
      }
    }

    def unapply(e: WithContext[Expression]): Option[(List[(WithContext[Expression], BoundVar)], WithContext[Expression])] = {
      Some(seqsAt(e))
    }

    def apply(ss: Seq[(WithContext[Expression], BoundVar)], en: WithContext[Expression]): Expression = apply(ss.map(p => (p._1.e, p._2)), en)
    def apply(ss: Seq[(Expression, BoundVar)], en: Expression): Expression = {
      ss match {
        case Nil => en
        case (e, x) +: sst => e > x > apply(sst, en)
      }
    }
  }
  */

  /*
  object IndependentSeqs {
    private def independent(p: WithContext[Expression], a: ExpressionAnalysisProvider[Expression]): (Seq[(Expression, BoundVar)], WithContext[Expression]) = {
      import a.ImplicitResults._
      val Seqs(seqs, core) = p
      def collectIndependents(
        seqs: List[(WithContext[Expression], BoundVar)],
        independents: Vector[(Expression, BoundVar)] = Vector[(Expression, BoundVar)](),
        ctx: TransformContext = core.ctx): (Seq[(Expression, BoundVar)], WithContext[Expression]) = seqs match {
        case (f, x) +: tl =>
          val isSafe = f.effectFree && f.nonBlockingPublish && (f.publications only 1)
          val noRefs = independents.map(_._2).forall { y => !f.freeVars.contains(y) }
          if (isSafe && noRefs) {
            // FIXME: This generates the context inside out. :/
            val _ > _ > (_ in newctx) = (f.e > x > core) in ctx
            collectIndependents(tl, independents :+ (f.e, x), newctx)
          } else {
            (independents, Seqs(seqs, core) in ctx)
          }
        case Nil =>
          (independents, core)
      }
      collectIndependents(seqs)
    }

    def unapply(p: (WithContext[Expression], ExpressionAnalysisProvider[Expression])): Option[(Seq[(Expression, BoundVar)], WithContext[Expression])] = {
      val (e, a) = p
      val (seqs, core) = independent(e, a)
      if (seqs.size >= 1)
        Some((seqs, core))
      else e match {
        case f > x > g => Some((List((f, x)), g))
        case _ => None
      }
    }
  }
  */

  /*

  /* This uses the identity:
   * future x = future y = e # f # g
   * ===
   * future y = e # future x = f # g
   */

  object Futures {
    private def futsAt(p: WithContext[Expression]): (List[(WithContext[Expression], BoundVar)], WithContext[Expression]) = {
      p match {
        case FutureAt(f) > x > g => {
          val (fss, fn) = futsAt(f) // f1 >x1> ... >xn-1> fn
          val (gss, gn) = futsAt(g) // g1 >y1> ... >yn-1> gn
          // TODO: Add an assert here to check that no variables are shadowed. This should never happen because of how variables are handled and allocated.
          // f1 >x1> ... >xn-1> fn >x> g1 >y1> ... >yn-1> gn
          (fss ::: (fn, x) :: gss, gn)
        }
        case e => (Nil, e)
      }
    }

    def unapply(e: WithContext[Expression]): Option[(List[(WithContext[Expression], BoundVar)], WithContext[Expression])] = {
      Some(futsAt(e))
    }

    def apply(ss: Seq[(WithContext[Expression], BoundVar)], en: WithContext[Expression]): Expression = apply(ss.map(p => (p._1.e, p._2)), en)
    def apply(ss: Seq[(Expression, BoundVar)], en: Expression): Expression = {
      ss match {
        case Nil => en
        case (e, x) +: sst => Future(e) > x > apply(sst, en)
      }
    }
  }

  /** Match an expression in the form: e <x1<| g1 <x2<| g2 ... where x1,...,xn are distinct
    * and no gi references any xj.
    */
  object IndependentFutures {
    private def independent(p: WithContext[Expression], a: ExpressionAnalysisProvider[Expression]): (Seq[(Expression, BoundVar)], WithContext[Expression]) = {
      import a.ImplicitResults._
      val Futures(seqs, core) = p
      def collectIndependents(
        seqs: List[(WithContext[Expression], BoundVar)],
        independents: Vector[(Expression, BoundVar)] = Vector[(Expression, BoundVar)](),
        ctx: TransformContext = core.ctx): (Seq[(Expression, BoundVar)], WithContext[Expression]) = seqs match {
        case (f, x) +: tl =>
          val noRefs = independents.map(_._2).forall { y => !f.freeVars.contains(y) }
          if (noRefs) {
            // FIXME: This generates the context inside out. :/
            val _ > _ > (_ in newctx) = (f.e > x > core) in ctx
            collectIndependents(tl, independents :+ (f.e, x), newctx)
          } else {
            (independents, Futures(seqs, core) in ctx)
          }
        case Nil =>
          (independents, core)
      }
      collectIndependents(seqs)
    }

    def unapply(p: (WithContext[Expression], ExpressionAnalysisProvider[Expression])): Option[(Seq[(Expression, BoundVar)], WithContext[Expression])] = {
      val (e, a) = p
      val (seqs, core) = independent(e, a)
      if (seqs.size >= 1)
        Some((seqs, core))
      else e match {
        case f > x > g => Some((List((f, x)), g))
        case _ => None
      }
    }
  }
  */
}

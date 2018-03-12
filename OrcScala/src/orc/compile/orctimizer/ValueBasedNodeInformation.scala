//
// ValueBasedNodeInformation.scala -- Scala class ValueBasedNodeInformation
// Project OrcScala
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.orctimizer

import orc.ast.orctimizer.named.{ Argument, Call, Constant, Method, Routine }
import orc.util.{ TFalse, TTrue, TUnknown }
import orc.values.sites.Effects

import CallGraphValues.{ NodeValue, ObjectValueSet }
import FlowGraph.{ ConstantNode, MethodNode, TokenFlowNode, ValueNode }

class ValueBasedNodeInformation(graph: CallGraph) {
  implicit final class CallGraph_TokenFlowNodeAdds(val node: TokenFlowNode) {
    def effects: Boolean = {
      node.location match {
        case Call.Z(target, args, _) => {
          val (a, b, c) = target.byCallTargetCases(
            externals = { vs =>
              // FIXME: Update to use compile time "invoker" API once available. This will avoid problems of too specific results since the Site assumes a specific arity, etc.
              vs.collect({
                case site: orc.values.sites.SpecificArity =>
                  if (args.size == site.arity) {
                    site.effects != Effects.None
                  } else {
                    false
                  }
                case site: orc.values.sites.Site =>
                  site.effects != Effects.None
                case _ =>
                  true
              }).reduce(_ || _)
            }, internals = { vs =>
              false
            }, others = { vs =>
              true
            })
          Seq(a, b, c).flatten.fold(false)(_ || _)
        }
        case _ => false
      }
    }
  }

  implicit final class CallGraph_ArgumentAdds(val arg: Argument.Z) {
    def byCallTargetCases[A, B, C](externals: Set[AnyRef] => A,
      internals: Set[Method.Z] => B, others: Set[CallGraphValues.Value[ObjectValueSet]] => C): (Option[A], Option[B], Option[C]) = {
      val possibleV = graph.valuesOf(ValueNode(arg))

      val extPubs = if (possibleV.exists({
        case n: NodeValue[_] => n.isExternalMethod.isTrue
        case _ => false
      })) {
        val vs = possibleV.toSet.collect {
          case n @ NodeValue(ConstantNode(Constant(site), _)) if n.isExternalMethod.isTrue => site
        }
        assert(vs.nonEmpty, s"Failed to get externals: $possibleV")
        Some(externals(vs))
      } else {
        None
      }

      val intPubs = if (possibleV.exists({
        case n: NodeValue[_] => n.isInternalMethod.isTrue
        case _ => false
      })) {
        val vs = possibleV.toSet.collect {
          case NodeValue(MethodNode(m, _)) => m
        }
        assert(vs.nonEmpty, s"Failed to get internals: $possibleV")
        Some(internals(vs))
      } else {
        None
      }

      val otherPubs = if (possibleV.exists({
        case n: NodeValue[_] if n.isMethod => false
        case _ => true
      })) {
        val vs = possibleV.toSet.flatMap({
          case n: NodeValue[_] if n.isMethod => None
          case v => Some(v)
        })
        assert(vs.nonEmpty, s"Failed to get others: $possibleV")
        Some(others(vs))
      } else {
        None
      }

      (extPubs, intPubs, otherPubs)
    }

    def byIfLenientCases[T](left: => T, right: => T, both: => T) = {
      val possibleV = graph.valuesOf(ValueNode(arg))

      val isDef = possibleV.view.map({
        case NodeValue(MethodNode(_: Routine.Z, _)) => TTrue
        case _ => TFalse
      }).fold(TUnknown)(_ union _)

      isDef match {
        case TTrue =>
          left
        case TFalse =>
          right
        case TUnknown =>
          both
      }
    }
  }
}

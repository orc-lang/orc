//
// NamedToNameless.scala -- Scala trait NamedToNameless
// Project OrcScala
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.oil.named

import orc.ast.oil.nameless

/** @author dkitchin
  */
// Conversions from named to nameless representations
trait NamedToNameless {

  def namedToNameless(e: Expression, context: List[BoundVar], typecontext: List[BoundTypevar]): nameless.Expression = {
    def toExp(e: Expression): nameless.Expression = namedToNameless(e, context, typecontext)
    def toArg(a: Argument): nameless.Argument = namedToNameless(a, context)
    def toType(t: Type): nameless.Type = namedToNameless(t, typecontext)
    e -> {
      case Stop() => nameless.Stop()
      case a: Argument => namedToNameless(a, context)
      case Call(target, args, typeargs) => nameless.Call(toArg(target), args map toArg, typeargs map { _ map toType })
      case left || right => nameless.Parallel(toExp(left), toExp(right))
      case left > x > right => nameless.Sequence(toExp(left), namedToNameless(right, x :: context, typecontext))
      case Graft(x, value, body) => nameless.Graft(toExp(value), namedToNameless(body, x :: context, typecontext))
      case Trim(f) => nameless.Trim(toExp(f))
      case left ow right => nameless.Otherwise(toExp(left), toExp(right))
      case New(self, st, bindings, t) => {
        // TODO: Consider compacting the context since we will.
        val newContext = self :: context
        val newBindings = Map() ++ bindings.mapValues(namedToNameless(_, newContext, typecontext))
        nameless.New(st map toType, newBindings, t map toType)
      }
      case FieldAccess(obj, field) => nameless.FieldAccess(namedToNameless(obj, context), field)
      case DeclareCallables(defs, body) => {
        val (defcontext, bodycontext, openvars) = compactContext(defs, context)
        val newdefs = defs map { namedToNameless(_, defcontext, typecontext) }
        val newbody = namedToNameless(body, bodycontext, typecontext)
        nameless.DeclareCallables(openvars, newdefs, newbody)
      }
      case DeclareType(x, t, body) => {
        val newTypeContext = x :: typecontext
        /* A type may be defined recursively, so its name is in scope for its own definition */
        val newt = namedToNameless(t, newTypeContext)
        val newbody = namedToNameless(body, context, newTypeContext)
        nameless.DeclareType(newt, newbody)
      }
      case HasType(body, expectedType) => nameless.HasType(toExp(body), toType(expectedType))
      case VtimeZone(timeOrder, body) => nameless.VtimeZone(toArg(timeOrder), toExp(body))
      case Hole(holeContext, holeTypeContext) => {
        val newHoleContext = holeContext mapValues { namedToNameless(_, context) }
        val newHoleTypeContext = holeTypeContext mapValues { namedToNameless(_, typecontext) }
        nameless.Hole(newHoleContext, newHoleTypeContext)
      }
    }
  }

  private def compactContext(clss: List[NamedDeclaration with hasFreeVars], context: List[BoundVar]): (List[BoundVar], List[BoundVar], List[Int]) = {
    val names = clss map { _.name }
    val opennames = (clss flatMap { _.freevars }).distinct filterNot { names contains _ }
    val declcontext = names.reverse ::: opennames ::: context
    val bodycontext = names.reverse ::: context
    val openvars =
      opennames map { x =>
        val i = context indexOf x
        assert(i >= 0, s"Failed to find variable $x while compacting $clss")
        i
      }
    (declcontext, bodycontext, openvars)
  }

  def namedToNameless(a: Argument, context: List[BoundVar]): nameless.Argument = {
    a -> {
      case Constant(v) => nameless.Constant(v)
      case (x: BoundVar) => {
        val i = context indexOf x
        assert(i >= 0, s"Cannot find bound variable $x ($i)")
        nameless.Variable(i)
      }
      case UnboundVar(s) => nameless.UnboundVariable(s)
      case undef => throw new scala.MatchError(orc.util.GetScalaTypeName(undef) + " not matched in NamedToNameless.namedToNameless(Argument, List[BoundVar])")
    }
  }

  def namedToNameless(t: Type, typecontext: List[BoundTypevar]): nameless.Type = {
    def toType(t: Type): nameless.Type = namedToNameless(t, typecontext)
    t -> {
      case u: BoundTypevar => {
        val i = typecontext indexOf u
        assert(i >= 0, t)
        nameless.TypeVar(i)
      }
      case Top() => nameless.Top()
      case Bot() => nameless.Bot()
      case FunctionType(typeformals, argtypes, returntype) => {
        val newTypeContext = typeformals ::: typecontext
        val newArgTypes = argtypes map { namedToNameless(_, newTypeContext) }
        val newReturnType = namedToNameless(returntype, newTypeContext)
        nameless.FunctionType(typeformals.size, newArgTypes, newReturnType)
      }
      case TupleType(elements) => nameless.TupleType(elements map toType)
      case RecordType(entries) => {
        val newEntries = entries map { case (s, t) => (s, toType(t)) }
        nameless.RecordType(newEntries)
      }
      case TypeApplication(tycon, typeactuals) => {
        val i = typecontext indexOf tycon
        assert(i >= 0)
        nameless.TypeApplication(i, typeactuals map toType)
      }
      case AssertedType(assertedType) => nameless.AssertedType(toType(assertedType))
      case TypeAbstraction(typeformals, t) => {
        val newTypeContext = typeformals ::: typecontext
        val newt = namedToNameless(t, newTypeContext)
        nameless.TypeAbstraction(typeformals.size, newt)
      }
      case ImportedType(classname) => nameless.ImportedType(classname)
      case ClassType(classname) => nameless.ClassType(classname)
      case VariantType(self, typeformals, variants) => {
        val newTypeContext = self :: typeformals ::: typecontext
        val newVariants =
          for ((name, variant) <- variants) yield {
            (name, variant map { namedToNameless(_, newTypeContext) })
          }
        nameless.VariantType(typeformals.size, newVariants)
      }
      case IntersectionType(a, b) => nameless.IntersectionType(toType(a), toType(b))
      case UnionType(a, b) => nameless.UnionType(toType(a), toType(b))
      case StructuralType(members) => nameless.StructuralType(members.mapValues(toType))
      case NominalType(a) => nameless.NominalType(toType(a))
      case UnboundTypevar(s) => nameless.UnboundTypeVariable(s)
      //case undef => throw new scala.MatchError(orc.util.GetScalaTypeName(undef) + " not matched in NamedToNameless.namedToNameless(Type, List[BoundTypeVar])")
    }
  }

  def namedToNameless(defn: Callable, context: List[BoundVar], typecontext: List[BoundTypevar]): nameless.Callable = {
    defn -> {
      case Callable(_, formals, body, typeformals, argtypes, returntype) => {
        val newContext = formals.reverse ::: context
        val newTypeContext = typeformals ::: typecontext
        val newbody = namedToNameless(body, newContext, newTypeContext)
        val newArgTypes = argtypes map { _ map { namedToNameless(_, newTypeContext) } }
        val newReturnType = returntype map { namedToNameless(_, newTypeContext) }
        defn match {
          case _: Def =>
            nameless.Def(typeformals.size, formals.size, newbody, newArgTypes, newReturnType)
          case _: Site =>
            nameless.Site(typeformals.size, formals.size, newbody, newArgTypes, newReturnType)
        }
      }
    }
  }

}

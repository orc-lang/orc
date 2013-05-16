//
// NamedToOrc5C.scala -- Scala class/trait/object NamedToOrc5C
// Project OrcScala
//
// $Id$
//
// Created by amp on Apr 29, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.oil.named.orc5c

import orc.ast.oil.named

/**
  *
  * @author amp
  */
object NamedToOrc5C {
  import named._
  
  def namedToOrc5C(
        e: Expression, 
        context: Map[BoundVar, orc5c.BoundVar], 
        typecontext: Map[BoundTypevar, orc5c.BoundTypevar]): orc5c.Expression = {
    def toExp(e: Expression): orc5c.Expression = namedToOrc5C(e, context, typecontext)
    def toArg(a: Argument): orc5c.Argument = namedToOrc5C(a, context)
    def toType(t: Type): orc5c.Type = namedToOrc5C(t, typecontext)
    e -> {
      case Stop() => orc5c.Stop()
      case a: Argument => namedToOrc5C(a, context)
      case Call(target, args, typeargs) => orc5c.Call(toArg(target), args map toArg, typeargs map { _ map toType })
      case left || right => orc5c.Parallel(toExp(left), toExp(right))
      case left > x > right =>
        val newx = new orc5c.BoundVar(x.optionalVariableName)
        orc5c.Sequence(toExp(left), newx, namedToOrc5C(right, context + ((x, newx)), typecontext))
      case left < x < right => 
        val newx = new orc5c.BoundVar(x.optionalVariableName)
        orc5c.LateBind(namedToOrc5C(left, context + ((x, newx)), typecontext), newx, Limit(toExp(right)))
      case left ow right => orc5c.Otherwise(toExp(left), toExp(right))
      case DeclareDefs(defs, body) => {
        val defnames = defs map { _.name }
        val newdefnames = defnames map { n => new orc5c.BoundVar(n.optionalVariableName) }
        val newcontext = context ++ (defnames zip newdefnames)
        val newdefs = defs map { namedToOrc5C(_, newcontext, typecontext) }
        val newbody = namedToOrc5C(body, newcontext, typecontext)
        orc5c.DeclareDefs(newdefs, newbody)
      }
      case DeclareType(x, t, body) => {
        val newx = new orc5c.BoundTypevar()
        val newTypeContext = typecontext + ((x, newx))
        /* A type may be defined recursively, so its name is in scope for its own definition */
        val newt = namedToOrc5C(t, newTypeContext)
        val newbody = namedToOrc5C(body, context, newTypeContext)
        orc5c.DeclareType(newx, newt, newbody)
      }
      case HasType(body, expectedType) => orc5c.HasType(toExp(body), toType(expectedType))
    }
  }

  def namedToOrc5C(a: Argument, context: Map[BoundVar, orc5c.BoundVar]): orc5c.Argument = {
    a -> {
      case Constant(v) => orc5c.Constant(v)
      case (x: BoundVar) => {
        context(x)
      }
      case UnboundVar(s) => orc5c.UnboundVar(s)
    case undef => throw new scala.MatchError(undef.getClass.getCanonicalName + " not matched in namedToOrc5C.namedToOrc5C(Argument, List[BoundVar])")
    }
  }

  def namedToOrc5C(t: Type, typecontext: Map[BoundTypevar, orc5c.BoundTypevar]): orc5c.Type = {
    def toType(t: Type): orc5c.Type = namedToOrc5C(t, typecontext)
    t -> {
      case u: BoundTypevar => {
        typecontext(u)
      }
      case Top() => orc5c.Top()
      case Bot() => orc5c.Bot()
      case FunctionType(typeformals, argtypes, returntype) => {
        val newFormals = typeformals map { t => new orc5c.BoundTypevar(t.optionalVariableName) }
        val newTypeContext = typecontext ++ (typeformals zip newFormals)
        val newArgTypes = argtypes map { namedToOrc5C(_, newTypeContext) }
        val newReturnType = namedToOrc5C(returntype, newTypeContext)
        orc5c.FunctionType(newFormals, newArgTypes, newReturnType)
      }
      case TupleType(elements) => orc5c.TupleType(elements map toType)
      case RecordType(entries) => {
        val newEntries = entries map { case (s, t) => (s, toType(t)) }
        orc5c.RecordType(newEntries)
      }
      case TypeApplication(tycon, typeactuals) => {
        orc5c.TypeApplication(toType(tycon), typeactuals map toType)
      }
      case AssertedType(assertedType) => orc5c.AssertedType(toType(assertedType))
      case TypeAbstraction(typeformals, t) => {
        val newFormals = typeformals map { t => new orc5c.BoundTypevar(t.optionalVariableName) }
        val newTypeContext = typecontext ++ (typeformals zip newFormals)
        val newt = namedToOrc5C(t, newTypeContext)
        orc5c.TypeAbstraction(newFormals, newt)
      }
      case ImportedType(classname) => orc5c.ImportedType(classname)
      case ClassType(classname) => orc5c.ClassType(classname)
      case VariantType(self, typeformals, variants) => {
        val newFormals = typeformals map { t => new orc5c.BoundTypevar(t.optionalVariableName) }
        val newself = new orc5c.BoundTypevar(self.optionalVariableName)
        val newTypeContext = typecontext ++ (typeformals zip newFormals) + ((self, newself))
        val newVariants =
          for ((name, variant) <- variants) yield {
            (name, variant map { namedToOrc5C(_, newTypeContext) })
          }
        orc5c.VariantType(newself, newFormals, newVariants)
      }
      case UnboundTypevar(s) => orc5c.UnboundTypevar(s)
      case undef => throw new scala.MatchError(undef.getClass.getCanonicalName + " not matched in namedToOrc5C.namedToOrc5C(Type, List[BoundTypeVar])")
    }
  }

  def namedToOrc5C(defn: Def, context: Map[BoundVar, orc5c.BoundVar], typecontext: Map[BoundTypevar, orc5c.BoundTypevar]): orc5c.Def = {
    defn -> {
      case Def(name, formals, body, typeformals, argtypes, returntype) => {
        val newFormals = formals map { t => new orc5c.BoundVar(t.optionalVariableName) }
        val newTypeFormals = typeformals map { t => new orc5c.BoundTypevar(t.optionalVariableName) }
        val newContext = context ++ (formals zip newFormals)
        val newTypeContext = typecontext ++ (typeformals zip newTypeFormals)
        val newbody = namedToOrc5C(body, newContext, newTypeContext)
        val newArgTypes = argtypes map { _ map { namedToOrc5C(_, newTypeContext) } }
        val newReturnType = returntype map { namedToOrc5C(_, newTypeContext) }
        val newName = context(name)
        orc5c.Def(newName, newFormals, newbody, newTypeFormals, newArgTypes, newReturnType)
      }
    }
  }
}
//
// SplitPrune.scala -- Scala class/trait/object SplitPrune
// Project OrcScala
//
// Created by amp on Sep 24, 2013.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.translate

import orc.ast.oil.{ named => named5c }
import orc.ast.oil4c.{ named => named4c }
import orc.error.compiletime.{ CompilationException, ContinuableSeverity }

/** Translate Orc4C into Orc5C by splitting the pruning combinator into latebind and limit.
  *
  * @author amp
  */
class SplitPrune(val reportProblem: CompilationException with ContinuableSeverity => Unit) {
  def apply(expr: named4c.Expression): named5c.Expression = namedToOrc5C(expr, Map(), Map())

  import named4c._

  def namedToOrc5C(
    e: Expression,
    context: Map[BoundVar, named5c.BoundVar],
    typecontext: Map[BoundTypevar, named5c.BoundTypevar]): named5c.Expression = {
    def toExp(e: Expression): named5c.Expression = namedToOrc5C(e, context, typecontext)
    def toArg(a: Argument): named5c.Argument = namedToOrc5C(a, context)
    def toType(t: Type): named5c.Type = namedToOrc5C(t, typecontext)
    e -> {
      case Stop() => named5c.Stop()
      case a: Argument => namedToOrc5C(a, context)
      case Call(target, args, typeargs) => named5c.Call(toArg(target), args map toArg, typeargs map { _ map toType })
      case Parallel(left, right) => named5c.Parallel(toExp(left), toExp(right))
      case Sequence(left, x, right) =>
        val newx = new named5c.BoundVar(x.optionalVariableName)
        named5c.Sequence(toExp(left), newx, namedToOrc5C(right, context + ((x, newx)), typecontext))
      case Prune(left, x, right) =>
        val newx = new named5c.BoundVar(x.optionalVariableName)
        named5c.LateBind(namedToOrc5C(left, context + ((x, newx)), typecontext), newx,
          named5c.Limit(toExp(right)))
      case Otherwise(left, right) => named5c.Otherwise(toExp(left), toExp(right))
      case DeclareDefs(defs, body) => {
        val defnames = defs map { _.name }
        val newdefnames = defnames map { n => new named5c.BoundVar(n.optionalVariableName) }
        val newcontext = context ++ (defnames zip newdefnames)
        val newdefs = defs map { namedToOrc5C(_, newcontext, typecontext) }
        val newbody = namedToOrc5C(body, newcontext, typecontext)
        named5c.DeclareDefs(newdefs, newbody)
      }
      case DeclareType(x, t, body) => {
        val newx = new named5c.BoundTypevar()
        val newTypeContext = typecontext + ((x, newx))
        /* A type may be defined recursively, so its name is in scope for its own definition */
        val newt = namedToOrc5C(t, newTypeContext)
        val newbody = namedToOrc5C(body, context, newTypeContext)
        named5c.DeclareType(newx, newt, newbody)
      }
      case HasType(body, expectedType) => named5c.HasType(toExp(body), toType(expectedType))
      case VtimeZone(ord, body) => named5c.VtimeZone(toArg(ord), toExp(body))
      case Hole(ctx, tctx) => named5c.Hole(ctx mapValues toArg, tctx mapValues toType)
    }
  }

  def namedToOrc5C(a: Argument, context: Map[BoundVar, named5c.BoundVar]): named5c.Argument = {
    a -> {
      case Constant(v) => named5c.Constant(v)
      case (x: BoundVar) => {
        context(x)
      }
      case UnboundVar(s) => named5c.UnboundVar(s)
      case undef => throw new scala.MatchError(undef.getClass.getCanonicalName + " not matched in namedToOrc5C.namedToOrc5C(Argument, List[BoundVar])")
    }
  }

  def namedToOrc5C(t: Type, typecontext: Map[BoundTypevar, named5c.BoundTypevar]): named5c.Type = {
    def toType(t: Type): named5c.Type = namedToOrc5C(t, typecontext)
    t -> {
      case u: BoundTypevar => {
        typecontext(u)
      }
      case Top() => named5c.Top()
      case Bot() => named5c.Bot()
      case FunctionType(typeformals, argtypes, returntype) => {
        val newFormals = typeformals map { t => new named5c.BoundTypevar(t.optionalVariableName) }
        val newTypeContext = typecontext ++ (typeformals zip newFormals)
        val newArgTypes = argtypes map { namedToOrc5C(_, newTypeContext) }
        val newReturnType = namedToOrc5C(returntype, newTypeContext)
        named5c.FunctionType(newFormals, newArgTypes, newReturnType)
      }
      case TupleType(elements) => named5c.TupleType(elements map toType)
      case RecordType(entries) => {
        val newEntries = entries map { case (s, t) => (s, toType(t)) }
        named5c.RecordType(newEntries)
      }
      case TypeApplication(tycon, typeactuals) => {
        named5c.TypeApplication(toType(tycon), typeactuals map toType)
      }
      case AssertedType(assertedType) => named5c.AssertedType(toType(assertedType))
      case TypeAbstraction(typeformals, t) => {
        val newFormals = typeformals map { t => new named5c.BoundTypevar(t.optionalVariableName) }
        val newTypeContext = typecontext ++ (typeformals zip newFormals)
        val newt = namedToOrc5C(t, newTypeContext)
        named5c.TypeAbstraction(newFormals, newt)
      }
      case ImportedType(classname) => named5c.ImportedType(classname)
      case ClassType(classname) => named5c.ClassType(classname)
      case VariantType(self, typeformals, variants) => {
        val newFormals = typeformals map { t => new named5c.BoundTypevar(t.optionalVariableName) }
        val newself = new named5c.BoundTypevar(self.optionalVariableName)
        val newTypeContext = typecontext ++ (typeformals zip newFormals) + ((self, newself))
        val newVariants =
          for ((name, variant) <- variants) yield {
            (name, variant map { namedToOrc5C(_, newTypeContext) })
          }
        named5c.VariantType(newself, newFormals, newVariants)
      }
      case UnboundTypevar(s) => named5c.UnboundTypevar(s)
      case undef => throw new scala.MatchError(undef.getClass.getCanonicalName + " not matched in namedToOrc5C.namedToOrc5C(Type, List[BoundTypeVar])")
    }
  }

  def namedToOrc5C(defn: Def, context: Map[BoundVar, named5c.BoundVar], typecontext: Map[BoundTypevar, named5c.BoundTypevar]): named5c.Def = {
    defn -> {
      case Def(name, formals, body, typeformals, argtypes, returntype) => {
        val newFormals = formals map { t => new named5c.BoundVar(t.optionalVariableName) }
        val newTypeFormals = typeformals map { t => new named5c.BoundTypevar(t.optionalVariableName) }
        val newContext = context ++ (formals zip newFormals)
        val newTypeContext = typecontext ++ (typeformals zip newTypeFormals)
        val newbody = namedToOrc5C(body, newContext, newTypeContext)
        val newArgTypes = argtypes map { _ map { namedToOrc5C(_, newTypeContext) } }
        val newReturnType = returntype map { namedToOrc5C(_, newTypeContext) }
        val newName = context(name)
        named5c.Def(newName, newFormals, newbody, newTypeFormals, newArgTypes, newReturnType)
      }
    }
  }
}
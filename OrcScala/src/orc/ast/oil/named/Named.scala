//
// Named.scala -- Named representation of OIL syntax
// Project OrcScala
//
// Created by dkitchin on May 28, 2010.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil.named

import orc.ast.{ AST, hasAutomaticVariableName, hasOptionalVariableName }
import orc.ast.oil.nameless
import orc.values

sealed abstract class NamedAST extends AST with NamedToNameless {
  def prettyprint() = (new PrettyPrint()).reduce(this).toString()
  override def toString() = prettyprint()

  override val subtrees: Iterable[NamedAST] = this match {
    case Call(target, args, typeargs) => target :: (args ::: typeargs.toList.flatten)
    case Parallel(left, right) => List(left, right)
    case Sequence(left, x, right) => List(left, x, right)
    case Graft(x, value, body) => List(x, value, body)
    case Trim(f) => List(f)
    case Otherwise(left, right) => List(left, right)
    case New(self, stpe, ds, tpe) => stpe ++ ds.values.toList ++ tpe
    case FieldAccess(o, f) => List(o)
    case DeclareCallables(defs, body) => defs ::: List(body)
    case VtimeZone(timeOrder, body) => List(timeOrder, body)
    case HasType(body, expectedType) => List(body, expectedType)
    case DeclareType(u, t, body) => List(u, t, body)
    case Callable(f, formals, body, typeformals, argtypes, returntype) => {
      f :: (formals ::: (List(body) ::: typeformals ::: argtypes.toList.flatten ::: returntype.toList))
    }
    case TupleType(elements) => elements
    case FunctionType(_, argTypes, returnType) => argTypes :+ returnType
    case TypeApplication(tycon, typeactuals) => tycon :: typeactuals
    case AssertedType(assertedType) => List(assertedType)
    case TypeAbstraction(typeformals, t) => typeformals ::: List(t)
    case RecordType(entries) => entries.values
    case VariantType(self, typeformals, variants) => {
      self :: typeformals ::: (for ((_, variant) <- variants; t <- variant) yield t)
    }
    case IntersectionType(a, b) => List(a, b)
    case UnionType(a, b) => List(a, b)
    case StructuralType(ms) => ms.values.toList
    case NominalType(a) => List(a)
    case Constant(_) | UnboundVar(_) | Hole(_, _) | Stop() => Nil
    case Bot() | ClassType(_) | ImportedType(_) | Top() | UnboundTypevar(_) => Nil
    case _: BoundVar | _: BoundTypevar => Nil
    //case undef => throw new scala.MatchError(undef.getClass.getCanonicalName + " not matched in NamedAST.subtrees")
  }

}

sealed abstract class Expression
  extends NamedAST
  with NamedInfixCombinators
  with hasVars
  with Substitution[Expression]
  with ContextualSubstitution
  with Guarding {
  lazy val withoutNames: nameless.Expression = namedToNameless(this, Nil, Nil)
}

sealed trait Declaration {
  this: hasOptionalVariableName =>
}

sealed trait NamedDeclaration extends Declaration {
  this: hasOptionalVariableName =>
  transferOptionalVariableName(name, this)
  val name: BoundVar
}

sealed case class Stop() extends Expression
sealed case class Call(target: Argument, args: List[Argument], typeargs: Option[List[Type]]) extends Expression
sealed case class Parallel(left: Expression, right: Expression) extends Expression
sealed case class Sequence(left: Expression, x: BoundVar, right: Expression) extends Expression
  with hasOptionalVariableName { transferOptionalVariableName(x, this) }
// Note: recommend reading Graft(x, f, g) as "graft x to f in g".
sealed case class Graft(x: BoundVar, value: Expression, body: Expression) extends Expression
  with hasOptionalVariableName { transferOptionalVariableName(x, this) }
sealed case class Trim(expr: Expression) extends Expression
sealed case class Otherwise(left: Expression, right: Expression) extends Expression
sealed case class DeclareCallables(defs: List[Callable], body: Expression) extends Expression {
  // The callables should contain all Sites or all Defs and not a mix.
}
// TODO: TYPECHECKER: Do we need mutually recursive types? Probably.
sealed case class DeclareType(name: BoundTypevar, t: Type, body: Expression) extends Expression
  with hasOptionalVariableName { transferOptionalVariableName(name, this) }
sealed case class HasType(body: Expression, expectedType: Type) extends Expression
object HasType {
  def optional(body: Expression, expectedType: Option[Type]): Expression =
    expectedType match {
      case Some(t) => HasType(body, t)
      case None => body
    }
}
sealed case class Hole(context: Map[String, Argument], typecontext: Map[String, Type]) extends Expression {
  def apply(e: Expression): Expression = e.subst(context, typecontext)
}
sealed case class VtimeZone(timeOrder: Argument, body: Expression) extends Expression

/** Construct a new object
  *
  * Objects have a self variable and structure. objType allows the association
  * between the object and the classes to be visible to the type checker. selfType
  * provides a type for the self reference.
  *
  * The bindings must result in an object with a structural type that is a subtype of
  * objType. The typechecker can enforce this.
  *
  * The translator will already have generated the proper bindings including implementing
  * inheritence correctly.
  *
  * Note with respect to DOT encoding:
  *
  * The objType parameter allows New to instantiate a nominal type. This is intentional.
  * DOT only supports nominal types when viewing a module from the outside and the nominal
  * hierarchy cannot be extended from outside a module. This mean standard OO open nominal
  * heirarchies are impossible in DOT.
  *
  * A global encoding would be possible by lifting each new operation into a global module
  * which also encodes the nominal types. See NominalType.
  */
sealed case class New(self: BoundVar, selfType: Option[Type], bindings: Map[values.Field, Expression], objType: Option[Type]) extends Expression

/** Read the value from a field future.
  *
  * This will block until the future is bound.
  */
sealed case class FieldAccess(obj: Argument, field: values.Field) extends Expression

/** Match an expression with exactly one hole.
  * Matches as Module(f), where f is a function which takes
  * a hole-filling expression and returns this expression
  * with the hole filled.
  */
object Module {
  def unapply(e: Expression): Option[Expression => Expression] = {
    if (countHoles(e) == 1) {
      def fillWith(fill: Expression): Expression = {
        val transform = new NamedASTTransform {
          override def onExpression(context: List[BoundVar], typecontext: List[BoundTypevar]) = {
            case h: Hole => h(fill)
          }
        }
        transform(e)
      }
      Some(fillWith)
    } else {
      None
    }
  }

  def countHoles(e: Expression): Int = {
    var holes = 0
    val search = new NamedASTTransform {
      override def onExpression(context: List[BoundVar], typecontext: List[BoundTypevar]) = {
        case h: Hole => holes += 1; h
      }
    }
    search(e)
    holes
  }
}

sealed abstract class Argument extends Expression
sealed case class Constant(value: AnyRef) extends Argument
sealed trait Var extends Argument with hasOptionalVariableName
sealed case class UnboundVar(name: String) extends Var {
  optionalVariableName = Some(name)
}

class BoundVar(optionalName: Option[String] = None) extends Var with hasAutomaticVariableName {

  optionalVariableName = optionalName
  autoName("v")
  assert(optionalVariableName.isDefined)

  def productIterator = optionalVariableName.toList.iterator
}

sealed abstract class Callable
  extends NamedAST
  with hasFreeVars
  with hasFreeTypeVars
  with hasOptionalVariableName
  with Substitution[Callable]
  with NamedDeclaration {
  lazy val withoutNames: nameless.Callable = namedToNameless(this, Nil, Nil)

  val name: BoundVar
  val formals: List[BoundVar]
  val body: Expression
  val typeformals: List[BoundTypevar]
  val argtypes: Option[List[Type]]
  val returntype: Option[Type]

  def copy(name: BoundVar = name,
    formals: List[BoundVar] = formals,
    body: Expression = body,
    typeformals: List[BoundTypevar] = typeformals,
    argtypes: Option[List[Type]] = argtypes,
    returntype: Option[Type] = returntype): Callable
}
object Callable {
  def unapply(value: Callable) = {
    Some((value.name, value.formals, value.body, value.typeformals, value.argtypes, value.returntype))
  }
}

case class Def(name: BoundVar, formals: List[BoundVar], body: Expression, typeformals: List[BoundTypevar], argtypes: Option[List[Type]], returntype: Option[Type]) extends Callable {
  def copy(name: BoundVar = name,
    formals: List[BoundVar] = formals,
    body: Expression = body,
    typeformals: List[BoundTypevar] = typeformals,
    argtypes: Option[List[Type]] = argtypes,
    returntype: Option[Type] = returntype): Def = {
    this ->> Def(name, formals, body, typeformals, argtypes, returntype)
  }
}

case class Site(name: BoundVar, formals: List[BoundVar], body: Expression, typeformals: List[BoundTypevar], argtypes: Option[List[Type]], returntype: Option[Type]) extends Callable {
  def copy(name: BoundVar = name,
    formals: List[BoundVar] = formals,
    body: Expression = body,
    typeformals: List[BoundTypevar] = typeformals,
    argtypes: Option[List[Type]] = argtypes,
    returntype: Option[Type] = returntype): Site = {
    this ->> Site(name, formals, body, typeformals, argtypes, returntype)
  }
}

sealed abstract class Type
  extends NamedAST
  with hasFreeTypeVars
  with Substitution[Type] {
  lazy val withoutNames: nameless.Type = namedToNameless(this, Nil)
}
sealed case class Top() extends Type
sealed case class Bot() extends Type
sealed case class TupleType(elements: List[Type]) extends Type
sealed case class RecordType(entries: Map[String, Type]) extends Type
sealed case class TypeApplication(tycon: Type, typeactuals: List[Type]) extends Type
sealed case class AssertedType(assertedType: Type) extends Type
sealed case class FunctionType(typeformals: List[BoundTypevar], argtypes: List[Type], returntype: Type) extends Type
sealed case class TypeAbstraction(typeformals: List[BoundTypevar], t: Type) extends Type
sealed case class ImportedType(classname: String) extends Type
sealed case class ClassType(classname: String) extends Type
sealed case class VariantType(self: BoundTypevar, typeformals: List[BoundTypevar], variants: List[(String, List[Type])]) extends Type

sealed case class IntersectionType(a: Type, b: Type) extends Type
object IntersectionType {
  def apply(as: Iterable[Type]): Type = {
    as.reduce(IntersectionType(_, _))
  }
}

sealed case class UnionType(a: Type, b: Type) extends Type
object UnionType {
  def apply(as: Iterable[Type]): Type = {
    as.reduce(UnionType(_, _))
  }
}

/** An explicitly nominal type which is a subtype of another type.
  *
  * Each instance of "NominalType(T)" is a distinct type.
  *
  * Note with respect to DOT encoding:
  *
  * This does not have a local encoding into DOT, but a global encoding is
  * possible. Each instance of nominal(T) in the program is lifted into a
  * single module in which all the nominal relationships can be encoded.
  * This single module is conceptually in scope for the entire program, but
  * is never realized in this compiler.
  *
  * See New.
  */
sealed case class NominalType(supertype: Type) extends Type

/** A structural object type
  *
  * This type is equivalent to an intersection of method member types in DOT.
  */
sealed case class StructuralType(members: Map[values.Field, Type]) extends Type

sealed trait Typevar extends Type with hasOptionalVariableName
sealed case class UnboundTypevar(name: String) extends Typevar {
  optionalVariableName = Some(name)
}
class BoundTypevar(optionalName: Option[String] = None) extends Typevar with hasAutomaticVariableName {

  optionalVariableName = optionalName
  autoName("t")

  def productIterator = optionalVariableName.toList.iterator
}

object Conversions {

  /** Given (e1, ... , en) and f, return:
    *
    * f(x1, ... , xn) <x1< e1
    *        ...
    *         <xn< en
    *
    * As an optimization, if any e is already an argument, no << binder is generated for it.
    */
  def unfold(es: List[Expression], makeCore: List[Argument] => Expression): Expression = {

    def expand(es: List[Expression]): (List[Argument], Expression => Expression) =
      es match {
        case (a: Argument) :: rest => {
          val (args, bindRest) = expand(rest)
          (a :: args, bindRest)
        }
        case g :: rest => {
          val (args, bindRest) = expand(rest)
          val x = new BoundVar()
          (x :: args, e => Graft(x, g, bindRest(e)))
        }
        case Nil => (Nil, e => e)
      }

    val (args, bind) = expand(es)
    bind(makeCore(args))
  }

  /** Given an expression of the form:
    *
    * E <x1<| e1
    * ...
    * <xn<| en
    *
    * where E is not a graft,
    * return E and (x1,e1), ... , (xn,en)
    *
    * If E is not of this form,
    * return E and Nil.
    */
  def partitionGraft(expr: Expression): (List[(Argument, Expression)], Expression) = {
    expr match {
      case Graft(x, value, body) => {
        val (bindings, core) = partitionGraft(body)
        ((x, value) :: bindings, core)
      }
      case _ => (Nil, expr)
    }
  }

}

/* Special syntactic forms, which conceptually 'reverse' some of
 * the translations performed earlier in compilation, because
 * it is sometimes easier to work with the unencoded versions.
 *
 * Each form is an object with an unapply (decode to special
 * form) and an apply (encode to canonical form) method. Thus,
 * they can be treated like case classes, except that construction
 * instantiates an entire subtree rather than a single class,
 * and similarly, deconstruction matches an entire subtree.
 */

/* A call with argument unfolding reversed.
 *
 * Matching this pattern can take O(N^2) steps,
 * where N is the depth of a series of left-associative
 * nested pruning combinators. That is, it is of the form
 * f <x1< g1 <x2< g2 ... <xN< gN
 *
 * The use cases for this pattern could be rewritten
 * to more complex and less maintainable O(N) solutions,
 * but I figured it wasn't worth it. -dkitchin
 */
object FoldedCall {

  def unapply(expr: Expression): Option[(Expression, List[Expression], Option[List[Type]])] = {
    Conversions.partitionGraft(expr) match {
      case (Nil, Call(target, args, typeArgs)) => Some((target, args, typeArgs))
      case (bindings, Call(target, args, typeArgs)) => {
        val exprMap = bindings.toMap
        if ((exprMap.keySet) subsetOf (args.toSet)) {
          val targetExpression = exprMap.getOrElse(target, target)
          val argExpressions = args map { arg => exprMap.getOrElse(arg, arg) }
          Some((targetExpression, argExpressions, typeArgs))
        } else {
          None
        }
      }
      case _ => None
    }
  }

  def apply(targetExpression: Expression, argExpressions: List[Expression], typeArgs: Option[List[Type]]): Expression = {
    Conversions.unfold(targetExpression :: argExpressions, { x => Call(x.head, x.tail, typeArgs) })
  }

}

/* A member access with target unfolding reversed.
 *
 */
object FoldedFieldAccess {

  def apply(targetExpression: Expression, f: values.Field): Expression = {
    Conversions.unfold(List(targetExpression), { x => FieldAccess(x.head, f) })
  }

}

/* An anonymous function with lambda translation reversed. */
object FoldedLambda {

  def unapply(expr: Expression): Option[(List[BoundVar], Expression, List[BoundTypevar], Option[List[Type]], Option[Type])] = {
    expr match {
      case DeclareCallables(List(Def(m, formals, body, typeFormals, argTypes, returnType)), n: BoundVar) if (m eq n) => {
        Some((formals, body, typeFormals, argTypes, returnType))
      }
      case _ => None
    }
  }

  /* FoldedLambda can only be constructed with full type annotations */
  def apply(formals: List[BoundVar], body: Expression, typeFormals: List[BoundTypevar], argTypes: Option[List[Type]], returnType: Option[Type]): Expression = {
    val dummyName = new BoundVar()
    val dummyDef = Def(dummyName, formals, body, typeFormals, argTypes, returnType)
    DeclareCallables(List(dummyDef), dummyName)
  }

}

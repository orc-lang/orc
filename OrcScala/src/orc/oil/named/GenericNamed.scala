//
// GenericNamed.scala -- Named representation of OIL syntax.
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 28, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//



// Note: This package uses parameterized AST classes to detect
//       ASTs which have vestigial string-named vars. It was a
//		 worthy experiment, but the typing is too heavyweight.

/*
package orc.oil.named

	import orc.oil._
	import orc.AST
	
	
	trait NamedAST[+X,+T] extends AST
	
	// The supertype of all scopes binding variables of type X
	trait Scope[X] { self: NamedAST[X,Any] => }

	// The supertype of all scopes binding type variables of type T
	trait TypeScope[T] { self: NamedAST[Any,T] => }
	 
	trait hasFreeVars[X] {
		val freevars: Set[X]

	}

	
	
	// X: The type of names
	// T: The type of type names
	abstract class Expression[X,T] extends NamedAST[X,T] with NamedInfixCombinators[X,T] with hasFreeVars[X]
	{ 
		lazy val stripNames: nameless.Expression = StripNames.stripNames(this, Nil, Nil)
		
		lazy val freevars:Set[X] = {
			this match {
			case x:X => Set(x)
			case Call(target,args,_) => target.freevars ++ args.flatMap(_.freevars)
			case Parallel(left,right) => left.freevars ++ right.freevars
			case Sequence(left,x,right) => left.freevars ++ (right.freevars - x)
			case Prune(left,x,right) => left.freevars ++ (right.freevars - x)
			case Otherwise(left,right) => left.freevars ++ right.freevars
			case DeclareDefs(defs,body) => (body.freevars ++ defs.flatMap(_.freevars)) -- defs.map(_.name)
			case HasType(body,typ) => body.freevars
			case _ => Set.empty
			}
		}
	}
	case class Stop extends Expression[Nothing,Nothing]
	case class Call[X,T](target: Argument[X], args: List[Argument[X]], typeargs: Option[List[Type[T]]]) extends Expression[X,T]
	case class Parallel[X,T](left: Expression[X,T], right: Expression[X,T]) extends Expression[X,T]
	case class Sequence[X,T](left: Expression[X,T], x: X, right: Expression[X,T]) extends Expression[X,T] with Scope[X]
	case class Prune[X,T](left: Expression[X,T], x: X, right: Expression[X,T]) extends Expression[X,T] with Scope[X]
	case class Otherwise[X,T](left: Expression[X,T], right: Expression[X,T]) extends Expression[X,T]
	case class DeclareDefs[X,T](defs : List[Def[X,T]], body: Expression[X,T]) extends Expression[X,T] with Scope[X]
	case class HasType[X,T](body: Expression[X,T], expectedType: Type[T]) extends Expression[X,T]
    
	abstract class Argument[+X] extends Expression[X,Nothing]
	case class Constant(value: Any) extends Argument[Nothing]
	
	
	case class Def[+X,+T](name: X, 
						  args: List[X], 
						  body: Expression[X,T], 
						  typeformals: List[T], 
						  argtypes: List[Type[T]], 
						  returntype: Option[Type[T]]) 
	extends NamedAST[X,T] 
	   with Scope[X] 
	   with TypeScope[T]
	   with hasFreeVars[X]
	{ 
		lazy val stripNames: nameless.Def = StripNames.stripNames(this, Nil, Nil)
		lazy val freevars: Set[X] = body.freevars -- args
	}
	
	
	abstract class Type[+T] extends NamedAST[Nothing,T]
	{ 
		lazy val stripNames: nameless.Type = StripNames.stripNames(this, Nil) 
	}	
	case class Top extends Type[Nothing]
	case class Bot extends Type[Nothing]
	case class NativeType(name: String) extends Type[Nothing]
	case class TupleType[T](elements: List[Type[T]]) extends Type[T]
	case class TypeApplication[T](tycon: T, typeactuals: List[Type[T]]) extends Type[T]
	case class AssertedType[T](assertedType: Type[T]) extends Type[T]	
	case class FunctionType[T](typeformals: List[T], 
							   argtypes: List[Type[T]], 
							   returntype: Type[T]) 
							   extends Type[T] 
							      with TypeScope[T]
	



// Conversions from named to nameless representations
object StripNames {
	 
	private def inverse_lookup[A](x: A, xs: List[A]): Int = 
		xs match { case (h::t) => if (x == h) { 0 } else { inverse_lookup(x,t) + 1 } }
	
	def stripNames[X,T](e: Expression[X,T], context: List[X], typecontext: List[T]): nameless.Expression = {
		def recurse(e: Expression[X,T]): nameless.Expression = stripNames(e, context, typecontext)
		e -> {
			case Stop() => nameless.Stop()
			case a : Argument[X] => stripNames(a, context)		
			case Call(target, args, typeargs) => {
				val newtarget = stripNames(target, context)
				val newargs = args map (stripNames(_, context))
				val newtypeargs = (typeargs.getOrElse(Nil)) map (stripNames(_, typecontext)) // Fragile. Oil should have optional type args.
				nameless.Call(newtarget, newargs, newtypeargs)
			}
			case left || right => nameless.Parallel(recurse(left), recurse(right))
			case left > x > right => nameless.Sequence(recurse(left), stripNames(right, x::context, typecontext))
			case left < x < right => nameless.Prune(stripNames(left, x::context, typecontext), recurse(right))
			case left ow right => nameless.Otherwise(recurse(left), recurse(right))
			case DeclareDefs(defs, body) => {
				val defnames = defs map (_.name)
				val newdefs = defs map (stripNames(_, defnames ::: context, typecontext))
				val newbody = stripNames(body, defnames ::: context, typecontext)
				nameless.DeclareDefs(newdefs, newbody)
			}
			case HasType(body, expectedType) => {
				nameless.HasType(recurse(body), stripNames(expectedType, typecontext))
			}
		} 
	}	
		
	def stripNames[X](a: Argument[X], context: List[X]): nameless.Argument =
		a -> {
			case Constant(v) => nameless.Constant(Literal(v))
			case (x: X) => nameless.Variable(inverse_lookup(x, context)) 
		}
	
		
	def stripNames[T](t: Type[T], typecontext: List[T]): nameless.Type = {
		t -> {
			case x: T => nameless.TypeVar(inverse_lookup(x, typecontext))
			case Top() => nameless.Top()
			case Bot() => nameless.Bot()
			case FunctionType(typeformals, argtypes, returntype) => {
				val newTypeContext = typeformals ::: typecontext
				val newArgTypes = argtypes map (stripNames(_, newTypeContext))
				val newReturnType = stripNames(returntype, newTypeContext)
				nameless.ArrowType(typeformals.size, newArgTypes, newReturnType)
			}
			case _ => nameless.Bot() 
			// Oil nameless type spec is incomplete:
			
			// case NativeType(name) =>
			// case TupleType(elements) =>
			// case TypeApplication(tycon, typeactuals) =>
			// case AssertedType(assertedType) =>
		}
	}	
	
	def stripNames[X,T](defn: Def[X,T], context: List[X], typecontext: List[T]): nameless.Def = {
		defn -> {
			case Def(_, args, body, typeformals, argtypes, returntype) => {
				val newContext = args ::: context
				val newTypeContext = typeformals ::: typecontext 
				val newbody = stripNames(body, newContext, newTypeContext)
				val newArgTypes = argtypes map (stripNames(_, newTypeContext))
				val newReturnType = returntype map (stripNames(_, newTypeContext))
				nameless.Def(typeformals.size, args.size, newbody, newArgTypes, newReturnType.get) // Fragile. Oil should have optional return types.
			}
		}
	}

	
	
	
}	
	
*/

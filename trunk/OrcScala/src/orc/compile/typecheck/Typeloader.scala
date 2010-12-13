//
// Typeloader.scala -- Scala object Typeloader
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Nov 29, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml.
//
package orc.compile.typecheck

import orc.ast.oil.{named => syntactic}
import orc.types._
import orc.values.sites.SiteClassLoading
import orc.compile.typecheck.Typechecker._
import orc.types.Variance._
import orc.error.compiletime.typing._
import orc.error.compiletime.UnboundTypeVariableException
import orc.util.OptionMapExtension._
import java.lang.{reflect => jvm}

import orc.lib.state.types.ArrayType
import orc.lib.state.types.RefType


/** 
 * Lift syntactic types to semantic types, or reify
 * semantic types to syntactic types.
 *
 * @author dkitchin
 */
object Typeloader extends SiteClassLoading {
  
  
  /**
   * Lift a syntactic type to a first-order semantic type.
   */
  def lift(t: syntactic.Type)(implicit typeContext: TypeContext, typeOperatorContext: TypeOperatorContext): Type = {
    t match {
      case u: syntactic.BoundTypevar => {
        (typeContext get u) match {
          case Some(t) => t
          case None => {
            (typeOperatorContext get u) match {
              case Some(t) => throw new FirstOrderTypeExpectedException(t.toString)
              case None => throw new UnboundTypeException(u.toString) // should never occur
            }
          }
        }
      }
      case syntactic.Top() => Top
      case syntactic.Bot() => Bot
      case syntactic.TupleType(elements) => TupleType(elements map lift)
      case syntactic.RecordType(entries) => RecordType(entries mapValues lift)
      case syntactic.AssertedType(assertedType) => lift(assertedType) 
      case syntactic.FunctionType(typeFormals, argTypes, returnType) => {
        val newTypeFormals = typeFormals map { u => new TypeVariable(u) }
        val typeBindings = typeFormals zip newTypeFormals
        val newTypeContext = typeContext ++ typeBindings
        val newArgTypes = argTypes map { lift(_)(newTypeContext, typeOperatorContext) }
        val newReturnType = lift(returnType)(newTypeContext, typeOperatorContext)
        FunctionType(newTypeFormals, newArgTypes, newReturnType)
      }
      case syntactic.TypeApplication(t, typeactuals) => {
        liftToOperator(t).operate(typeactuals map lift)
      }
      case syntactic.ImportedType(classname) => loadType(classname)
      case syntactic.ClassType(classname) => {
        val cl = loadClass(classname)
        liftJavaType(cl)
      }
      case t@ syntactic.VariantType(Nil, variants) => {
        val newVariants = 
          for ((name, variant) <- variants) yield {
            (name, variant map lift)
          }
        val d = new MonomorphicDatatype(newVariants.toList)
        d.optionalDatatypeName = t.optionalVariableName
        d
      }
      case syntactic.UnboundTypevar(name) => {
        throw new UnboundTypeException(name)
      }
      case t => throw new FirstOrderTypeExpectedException(t.toString)
    }
  }
  
  /**
   * Lift a syntactic type to a second-order semantic type.
   */ 
  def liftToOperator(t: syntactic.Type)(implicit typeContext: TypeContext, typeOperatorContext: TypeOperatorContext): TypeOperator = {
    t match {
      case u: syntactic.BoundTypevar => {
        (typeOperatorContext get u) match {
          case Some(op) => op
          case None => {
            (typeContext get u) match {
              case Some(tt) => throw new SecondOrderTypeExpectedException(tt.toString)
              case None => throw new UnboundTypeException(u.toString) // should never occur
            }
          }
        }
      }
      case tt@ syntactic.VariantType(typeformals, variants) if (!typeformals.isEmpty) => {
        val newTypeFormals = typeformals map { u => new TypeVariable(u) }
        val typeBindings = typeformals zip newTypeFormals
        val newTypeContext = typeContext ++ typeBindings
        val newVariants = 
          for ((name, variant) <- variants) yield {
            val newVariant = variant map { lift(_)(newTypeContext, typeOperatorContext) }
            (name, newVariant)
          }
        val d = new PolymorphicDatatype(newTypeFormals, newVariants)
        d.optionalDatatypeName = tt.optionalVariableName
        new TypeOperator { def operate(ts: List[Type]) = TypeInstance(d, ts) }
      }
      case syntactic.TypeAbstraction(typeformals, t) => {
        val newTypeFormals = typeformals map { u => new TypeVariable(u) }
        val typeBindings = typeformals zip newTypeFormals
        val newTypeContext = typeContext ++ typeBindings
        val newT = lift(t)(newTypeContext, typeOperatorContext)
        new TypeOperator { def operate(ts: List[Type]) = newT.subst(ts, newTypeFormals) }
      }
      case syntactic.ImportedType(classname) => {
        loadTypeOperator(classname)
      }
      case syntactic.ClassType(classname) => {
        val cl = loadClass(classname)
        liftJavaTypeOperator(cl)
      }
      case syntactic.UnboundTypevar(name) => {
        throw new UnboundTypeException(name)
      }
      case tt => {
        throw new SecondOrderTypeExpectedException(tt.toString)
      }
    }
  }
    
  /**
   * Lift a syntactic type to either a first or second order type;
   * the kind is not known in advance.
   */
  def liftEither(t: syntactic.Type)(implicit typeContext: TypeContext, typeOperatorContext: TypeOperatorContext): Either[Type, TypeOperator] = {
    try {
      Left(lift(t))
    }
    catch {
      case _ : FirstOrderTypeExpectedException | _ : TypeResolutionException => {
        Right(liftToOperator(t))
      }
    }
  }
  
  
  /**
   * Lift a Java object type (represented by a Java class with no type parameters)
   * to an Orc type.
   */
  def liftJavaType(jt: jvm.Type, jctx: Map[jvm.TypeVariable[_], Type] = Nil.toMap): Type = {
    jt match {
      // C
      case cl: Class[_] => {
        // Make sure the parameters have been set in the context;
        // otherwise we are probably trying to lift a second-order type
        // by mistake.
        for (x <- cl.getTypeParameters()) {
          if (!(jctx contains x)) {
            throw new FirstOrderTypeExpectedException(cl.getCanonicalName())
          }
        }
        // Check if this is actually a primitive array class
        if (cl.isArray()) {
          val T = liftJavaType(cl.getComponentType(), jctx)
          ArrayType(T)
        }
        else if (java.lang.Boolean.TYPE isAssignableFrom cl) {
          BooleanType
        }
        else {
          JavaObjectType(cl)
        }
        /*TODO: Add explicit conversions from Java primitive types to Orc primitive types,
         *      or add appropriate subtyping in JavaObjectType and in the primitive types.
         */
      }
      // X
      case x: jvm.TypeVariable[_] => {
        x.getBounds().toList match {
          case List(b: Class[_]) if b.isAssignableFrom(classOf[Object]) => {
            jctx(x)
          }
          case _ => {
            throw new NoJavaTypeBoundsException()
          }
        }
      }
      // ?
      case w: jvm.WildcardType => {
        w.getUpperBounds().toList match {
          case List(upperBound: Class[_]) if upperBound.isAssignableFrom(classOf[Object]) => {
            Top
          }
          case _ => {
            throw new NoJavaTypeBoundsException()
          }
        }
      }
      // C<D>
      case pt: jvm.ParameterizedType => {
        val typeActuals = pt.getActualTypeArguments.toList map { liftJavaType(_, jctx) }
        val typeOp = liftJavaTypeOperator(pt.getRawType())
        typeOp.operate(typeActuals)
      }
      // C[]
      case gat: jvm.GenericArrayType => {
        val T = liftJavaType(gat.getGenericComponentType(), jctx)
        ArrayType(T)
      }
    }
  }
  
  
  
  def liftJavaClassType(cl: Class[_]) = JavaClassType(cl)
  
  
  /**
   * Lift a Java generic type (represented by a Java class with type parameters)
   * to an Orc type operator.
   */
  def liftJavaTypeOperator(jt: jvm.Type): TypeOperator = {
    
    jt match {
      case cl: Class[_] => {
        val formals = cl.getTypeParameters().toList
        if (formals.isEmpty) {
          throw new SecondOrderTypeExpectedException(cl.getCanonicalName())
        }
        else {
          val name = cl.getName()
          val variances = for (_ <- formals) yield Invariant
          new SimpleTypeConstructor(name, variances: _*) {

            override def instance(actuals: List[Type]): Type = {
              liftJavaType(cl, (formals zip actuals).toMap)  
            }
            
          }
        }
      }
      // At present, due to the type kinding assumptions of the Orc typechecker,
      // (namely, that type arguments are always first order), we can only allow
      // raw generic classes as type operators.
      case _ => throw new SecondOrderTypeExpectedException(jt.toString())
    }
    
  }
  
  
  
  def liftJavaField(jf: jvm.Field, jctx: Map[jvm.TypeVariable[_], Type]): Type = {
    val javaT = jf.getGenericType()
    val T = liftJavaType(javaT, jctx)
    RefType(T)
  }
  
  
  def liftJavaMethod(jm: jvm.Method, jctx: Map[jvm.TypeVariable[_], Type]): CallableType = {
    val javaTypeFormals = jm.getTypeParameters().toList
    val javaArgTypes = jm.getGenericParameterTypes().toList
    val javaReturnType = jm.getGenericReturnType()
    
    val newTypeFormals = javaTypeFormals map { jx => new TypeVariable(Some(jx.getName())) }
    val newJavaContext = jctx ++ (javaTypeFormals zip newTypeFormals)
    val newArgTypes = javaArgTypes map { liftJavaType(_, newJavaContext) } 
    val newReturnType = liftJavaType(javaReturnType, newJavaContext)
    
    FunctionType(newTypeFormals, newArgTypes, newReturnType)
  }
  
  
  /** 
   * This function converts a semantic type back to a syntactic type.
   * 
   * The result is optional, since some semantic types will have
   * no syntactic representation.
   * 
   * This conversion performs reverse lookups on typing contexts
   * to find the syntactic type variable which originally bound
   * the semantic type. If the result is not unique, reification fails.
   * 
   * @author dkitchin
   */
  def reify(that: Type)(implicit typeContext: TypeContext, typeOperatorContext: TypeOperatorContext): Option[syntactic.Type] = {
    that match {
      case Top => Some(syntactic.Top())
      case Bot => Some(syntactic.Bot())
      case IntegerConstantType(_) => reify(IntegerType)     
      case TupleType(elements) => {
        val newElements = elements optionMap reify
        newElements match {
          case Some(syntacticElements) => {
            Some(syntactic.TupleType(syntacticElements)) 
          }
          case _ => None
        }
      }
      case RecordType(entries) => {
        val newEntries = 
          entries mapValues {
            reify(_) match {
              case Some(u) => u
              case None => return None
            }
          }
        Some(syntactic.RecordType(newEntries))
      }
      case FunctionType(typeFormals, argTypes, returnType) => {
        val syntacticTypeFormals = typeFormals map { u => new syntactic.BoundTypevar(u.optionalVariableName) }
        val typeBindings = syntacticTypeFormals zip typeFormals
        val newTypeContext = typeContext ++ typeBindings // exploit reverse lookup to restore syntactic parameters
        val newArgTypes = argTypes optionMap { reify(_)(newTypeContext, typeOperatorContext) }
        val newReturnType = reify(returnType)(newTypeContext, typeOperatorContext) 
        (newArgTypes, newReturnType) match {
          case (Some(syntacticArgTypes), Some(syntacticReturnType)) => {
            Some(syntactic.FunctionType(syntacticTypeFormals, syntacticArgTypes, syntacticReturnType))
          }
          case _ => None
        }
      }
      case TypeInstance(tycon, args) => {
        val newTycon = reverseLookup(typeOperatorContext, tycon)
        val newArgs = args optionMap reify
        (newTycon, newArgs) match {
          case (Some(syntacticTycon), Some(syntacticArgs)) => {
            Some(syntactic.TypeApplication(syntacticTycon, syntacticArgs))
          }
          case _ => None
        }
      }
      case t => {
        reverseLookup(typeContext, t)
      }
    }
  }

  
  def reverseLookup[X,Y](m: Map[X, Y], target: Y): Option[X] = {
    val candidates = m filter { case (x, y) => y equals target }
    candidates.keys.toList match {
      case Nil => None
      case List(x) => Some(x)
      case alts => None // TODO Issue warning: multiple candidates
    }
  }
  
  
  
  /**
   * 
   * Given the name of a subtype of Type, instantiate that class
   * as a type.
   * 
   */
  def loadType(name: String): Type = {
    val loadedClass = loadClass(name)
    if (classOf[Type] isAssignableFrom loadedClass) {
      try {
        return loadedClass.asInstanceOf[Class[Type]].newInstance()
      } 
      catch {
        case e => throw new TypeResolutionException(loadedClass.getName(), e)
      }
    } 
    else {
      try { // Maybe it's a Scala object....
        val loadedClassCompanion = loadClass(name+"$")
        return loadedClassCompanion.getField("MODULE$").get(null).asInstanceOf[Type]
      } 
      catch {
        case _ => { } //Ignore -- It's not a Scala object, then.
      }
      throw new TypeResolutionException(loadedClass.getName(),new ClassCastException(loadedClass.getCanonicalName()+" cannot be cast to "+classOf[Type].getCanonicalName()))
    }
  }
  
  
  /**
   * 
   * Given the name of a subtype of TypeOperator, instantiate that class
   * as a type operator.
   * 
   */
  def loadTypeOperator(name: String): TypeOperator = {
    val loadedClass = loadClass(name)
    if (classOf[TypeOperator] isAssignableFrom loadedClass) {
      try {
        return loadedClass.asInstanceOf[Class[TypeOperator]].newInstance()
      } 
      catch {
        case e => throw new TypeOperatorResolutionException(loadedClass.getName(), e)
      }
    } 
    else {
      try { // Maybe it's a Scala object....
        val loadedClassCompanion = loadClass(name+"$")
        return loadedClassCompanion.getField("MODULE$").get(null).asInstanceOf[TypeOperator]
      } 
      catch {
        case _ => { } //Ignore -- It's not a Scala object, then.
      }
      throw new TypeOperatorResolutionException(loadedClass.getName(),new ClassCastException(loadedClass.getCanonicalName()+" cannot be cast to "+classOf[TypeOperator].getCanonicalName()))
    }
  }
  
  
}

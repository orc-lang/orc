//
// TypeExceptions.scala -- Scala child classes of TypeException
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Aug 11, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.error.compiletime.typing

import orc.types.Type
import orc.values.Field
import orc.error.compiletime._

class ArgumentArityException(val arityExpected: Int, val arityReceived: Int) extends TypeException("Expected " + arityExpected + " arguments to call, got " + arityReceived + " arguments instead.") with SeverityError

class TypeArgumentArityException(val arityExpected: Int, val arityReceived: Int) extends TypeException("Expected " + arityExpected + " type arguments to call, got " + arityReceived + " arguments instead.") with SeverityError

class DefinitionArityException(val arityFromType: Int, val arityFromSyntax: Int) extends TypeException("Definition should have " + arityFromType + " arguments according to its type, observed " + arityFromSyntax + " arguments instead.") with SeverityError

class ArgumentTypecheckingException(val argPosition: Int, val expectedType: Type, val providedType: Type) extends TypeException("Expected type " + expectedType + " or some subtype for argument " + argPosition + ", got " + providedType + " instead")

/* Stub type for error messages to describe a missing type which might not be constructible
 * directly as a type, for example "a tuple of any size".
 */
case class ExpectedType(description: String) extends Type { override def toString = description }

class MissingTypeException() extends TypeException("Type checker failed: couldn't obtain sufficient type information from a service or value.") with SeverityError

class OverloadedTypeException(val specificMessages: List[String] = Nil) extends TypeException("All alternatives for overloaded type failed to typecheck.\n" + specificMessages.mkString("\n")) with SeverityError {
  def addAlternative(t: Type, e: TypeException) =
    new OverloadedTypeException((t + " failed due to error: \n" + e) :: specificMessages)
}

class SubtypeFailureException(val expectedType: Type, val receivedType: Type) extends TypeException("Expected type " + expectedType + " or some subtype, found type " + receivedType + " instead.") with SeverityError

class FunctionTypeExpectedException(val receivedType: Type) extends TypeException("Expected a function type, found type " + receivedType + " instead.") with SeverityError

class TypeArityException(val arityExpected: Int, val arityReceived: Int) extends TypeException("Expected " + arityExpected + " arguments to type instantiation, got " + arityReceived + " arguments instead.") with SeverityError

class UnboundTypeException(val typeName: String) extends TypeException("Type " + typeName + " is undefined") with SeverityError

class UncallableTypeException(val t: Type) extends TypeException("Type " + t + " cannot be called as a site or function.") with SeverityError

class UnrepresentableTypeException(val t: Type) extends TypeException(t.toString() + " has no concrete representation.") with SeverityError

class TypeArgumentInferenceFailureException() extends TypeException("Could not infer missing type arguments; please add explicit type arguments") with SeverityError

class UnspecifiedArgTypesException() extends TypeException("Could not infer missing argument types; please add argument type annotations") with SeverityError

class UnspecifiedReturnTypeException() extends TypeException("Could not infer missing return type; please add a return type annotation") with SeverityError

class FirstOrderTypeExpectedException(val nonFirstOrderType: String) extends TypeException("Kinding error: expected a first-order type, found " + nonFirstOrderType + " instead.") with SeverityError

class SecondOrderTypeExpectedException(val nonSecondOrderType: String) extends TypeException("Kinding error: expected a type operator, found " + nonSecondOrderType + " instead.") with SeverityError

class NoMinimalTypeWarning(val bestGuess: Type) extends TypeException("Could not infer a type argument; no minimal type found. Used " + bestGuess.toString() + " as a best guess. Please add explicit type arguments.") with SeverityWarning

class OverconstrainedTypeVariableException() extends TypeException("A type argument is overconstrained; inference failed. Please add explicit type arguments. There may also be an underlying type error.") with SeverityError

class NoBoundedPolymorphismException() extends TypeException("Bounded polymorphism is not yet supported by the Orc typechecker.")

class NoJavaTypeBoundsException() extends TypeException("Can't handle nontrivial type bounds (... extends T) on Java types; bounded polymorphism is not yet supported by the Orc typechecker.")

class TypeResolutionException(val typeName: String, cause: Throwable)
  extends CompilationException("Problem loading type " + typeName + ": " + cause.getMessage(), cause) with SeverityError

class TypeOperatorResolutionException(val typeOperatorName: String, cause: Throwable)
  extends CompilationException("Problem loading type operator " + typeOperatorName, cause) with SeverityError

class TupleSizeException(val sizeExpected: Int, val sizeReceived: Int) extends TypeException("Expected a tuple of size " + sizeExpected + ", got size " + sizeReceived + " instead.") with SeverityError

class RecordShapeMismatchException(val nonconformingRecordType: orc.types.RecordType, val missingField: String) extends TypeException("Type " + nonconformingRecordType + " is missing field " + missingField + ", which is required by this record pattern.") with SeverityError

class NoSuchMemberException(t: Type, missingMember: String) extends TypeException("Type " + t + " has no member named " + missingMember) with SeverityError

class MalformedDatatypeCallException() extends TypeException("Expected an instance of the datatype as a type argument")

class NoMatchingConstructorException() extends TypeException("No matching constructor found for the types of these arguments")

//
// ExecutionExceptions.scala -- Scala subclasses of ExecutionException
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
package orc.error.runtime

import scala.util.parsing.input.NoPosition

////////
// Token exceptions
////////

class RuntimeSupportException(name: String) extends TokenException("This runtime does not support '" + name + "'.")

/** Access denied to an operation because the engine does not have a required right
  */
class RightException(val rightName: String) extends TokenException("This execution does not have the right '" + rightName + "'")

/**
  */
class StackLimitReachedError(val limit: Int) extends TokenException("Stack limit (limit=" + limit + ") reached")

/**
  */
class TokenLimitReachedError(val limit: Int) extends TokenError("Token limit (limit=" + limit + ") reached")

/**
  */
class UncaughtException(message: String, cause: Throwable) extends TokenException(message, cause)

////////
// Runtime type exceptions
////////

/** Superclass of all runtime type exceptions, including arity mismatches,
  * argument type mismatches, and attempts to call uncallable values.
  */
abstract class RuntimeTypeException(message: String) extends TokenException(message)

/** per JLS section 15.12.2.5
  */
class AmbiguousInvocationException(val methodNames: Array[String]) extends RuntimeTypeException("Ambiguous method invocation: " + methodNames.mkString("  -OR-  "))

/**
  */
class ArgumentTypeMismatchException(val argPosition: Int, val expectedType: String, val providedType: String) extends RuntimeTypeException("Expected type " + expectedType + " for argument " + argPosition + ", got " + providedType + " instead")

/**
  */
class ArityMismatchException(val arityExpected: Int, val arityProvided: Int) extends RuntimeTypeException("Arity mismatch, expected " + arityExpected + " arguments, got " + arityProvided + " arguments.")

/**
  */
class InsufficientArgsException(val missingArg: Int, val arityProvided: Int) extends RuntimeTypeException("Arity mismatch, could not find argument " + missingArg + ", only got " + arityProvided + " arguments.")

/**
  */
class MalformedArrayAccessException(val args: List[AnyRef]) extends RuntimeTypeException("Array access requires a single Integer as an argument")

class BadArrayElementTypeException(val badType: String) extends TokenException("Unrecognized array element type: " + badType)

/**
  */
class TupleIndexOutOfBoundsException(val index: Int) extends RuntimeTypeException("Tuple index out of range: " + index)

/**
  */
class MethodTypeMismatchException(val methodName: String, val clazz: Class[_]) extends RuntimeTypeException("Argument types did not match any implementation for method '" + methodName + "' in " + clazz.getName() + ".")

/** Exception raised when an uncallable value occurs in call argPosition.
  */
class UncallableValueException(val uncallable: Any) extends RuntimeTypeException("Value not callable: \"" + uncallable.toString() + "\"")

/** Attempted dot access at an unknown member.
  */
class NoSuchMemberException(val v: AnyRef, val unknownMember: String) extends RuntimeTypeException("Value " + v + " does not have a '" + unknownMember + "' member")

////////
// Site exceptions
////////

/** A container for Java-level exceptions raised by code
  * implementing sites. These are wrapped as Orc exceptions
  * to localize the failure to the calling token.
  */
class JavaException(cause: Throwable) extends SiteException(cause.toString(), cause) {
  /** @return "position: ClassName: detailMessage (newline) position.longString"
    */
  override def getMessageAndPositon(): String = {
    if (getPosition() != null && getPosition() != NoPosition) {
      getPosition().toString() + ": " + getCause().toString() + "\n" + getPosition().longString
    } else {
      getCause().toString()
    }
  }

  /** @return "position: ClassName: detailMessage (newline) position.longString (newline) Orc stack trace... (newline) Java stack trace..."
    */
  override def getMessageAndDiagnostics(): String = {
    getMessageAndPositon() + "\n" + getOrcStacktraceAsString() + getJavaStacktraceAsString(getCause());
  }
}

class ProgramSignalledError(message: String) extends SiteException(message)

class VirtualClockError(val message: String) extends TokenError(message)

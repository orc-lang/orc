//
// ExecutionExceptions.scala -- Scala subclasses of ExecutionException
// Project OrcScala
//
// Created by jthywiss on Aug 11, 2010.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.error.runtime

////////
// Token exceptions
////////

@SerialVersionUID(-7127776083186242849L)
class RuntimeSupportException(name: String) extends TokenException("This runtime does not support '" + name + "'.")

/** Access denied to an operation because the engine does not have a required right
  */
@SerialVersionUID(6056243842051122378L)
class RightException(val rightName: String) extends TokenException("This execution does not have the right '" + rightName + "'")

/**
  */
@SerialVersionUID(-3489699794846185110L)
class StackLimitReachedError(val limit: Int) extends TokenException("Stack limit (limit=" + limit + ") reached")

/**
  */
@SerialVersionUID(4878999138940046944L)
class TokenLimitReachedError(val limit: Int) extends TokenError("Token limit (limit=" + limit + ") reached")

/**
  */
@SerialVersionUID(-2970321937465541662L)
class UncaughtException(message: String, cause: Throwable) extends TokenException(message, cause)

////////
// Runtime type exceptions
////////

/** Superclass of all runtime type exceptions, including arity mismatches,
  * argument type mismatches, and attempts to call uncallable values.
  */
@SerialVersionUID(5124192037935330699L)
abstract class RuntimeTypeException(message: String) extends TokenException(message)

/** per JLS section 15.12.2.5
  */
@SerialVersionUID(3342109741625932408L)
class AmbiguousInvocationException(val methodNames: Array[String]) extends RuntimeTypeException("Ambiguous method invocation: " + methodNames.mkString("  -OR-  "))

/**
  */
@SerialVersionUID(-6230353618255168714L)
class ArgumentTypeMismatchException(val argPosition: Int, val expectedType: String, val providedType: String) extends RuntimeTypeException("Expected type " + expectedType + " for argument " + argPosition + ", got " + providedType + " instead")

/**
  */
@SerialVersionUID(-7825679504377197387L)
class ArityMismatchException(val arityExpected: Int, val arityProvided: Int) extends RuntimeTypeException("Arity mismatch, expected " + arityExpected + " arguments, got " + arityProvided + " arguments.")

/**
  */
@SerialVersionUID(-7516787330720654083L)
class InsufficientArgsException(val missingArg: Int, val arityProvided: Int) extends RuntimeTypeException("Arity mismatch, could not find argument " + missingArg + ", only got " + arityProvided + " arguments.")

/**
  */
@SerialVersionUID(3541986342001858412L)
class MalformedArrayAccessException(val args: List[AnyRef]) extends RuntimeTypeException("Array access requires a single Integer as an argument")

@SerialVersionUID(3367335446781496570L)
class BadArrayElementTypeException(val badType: String) extends TokenException("Unrecognized array element type: " + badType)

/**
  */
@SerialVersionUID(6245597789513774903L)
class TupleIndexOutOfBoundsException(val index: Int) extends RuntimeTypeException("Tuple index out of range: " + index)

/**
  */
@SerialVersionUID(-803260225175583163L)
class MethodTypeMismatchException(val methodName: String, val clazz: Class[_]) extends RuntimeTypeException("Argument types did not match any implementation for method '" + methodName + "' in " + clazz.getName() + ".")

/** Exception raised when an uncallable value occurs in call argPosition.
  */
@SerialVersionUID(7171287004340017499L)
class UncallableValueException(val uncallable: Any) extends RuntimeTypeException("Value not callable: \"" + uncallable.toString() + "\"")

/** Attempted dot access at an unknown member.
  */
@SerialVersionUID(7027861135377746868L)
class NoSuchMemberException(val v: AnyRef, val unknownMember: String) extends RuntimeTypeException("Value " + v + " does not have a '" + unknownMember + "' member")

/** Attempted dot access to members on an object without members
  */
// TODO: Add SerialVersionUID
class DoesNotHaveMembersException(val v: AnyRef) extends RuntimeTypeException("Value " + v + " does not have members")

////////
// Site exceptions
////////

/** A container for Java-level exceptions raised by code
  * implementing sites. These are wrapped as Orc exceptions
  * to localize the failure to the calling token.
  */
@SerialVersionUID(1780402298269182364L)
class JavaException(cause: Throwable) extends SiteException(cause.toString(), cause) {
  /** @return "position: ClassName: detailMessage (newline) position.lineContentWithCaret [if available]"
    */
  override def getMessageAndPositon(): String = {
    if (getPosition() != null) {
      getPosition().toString() + ": " + getCause().toString() + (if (getPosition().lineContentWithCaret.equals("\n^")) "" else "\n" + getPosition().lineContentWithCaret)
    } else {
      getCause().toString()
    }
  }

  /** @return "position: ClassName: detailMessage (newline) position.lineContentWithCaret [if available] (newline) Orc stack trace... (newline) Java stack trace..."
    */
  override def getMessageAndDiagnostics(): String = {
    getMessageAndPositon() + "\n" + getOrcStacktraceAsString() + getJavaStacktraceAsString(getCause());
  }
}

@SerialVersionUID(5623326262754466753L)
class ProgramSignalledError(message: String) extends SiteException(message)

@SerialVersionUID(-1814816409189456821L)
class VirtualClockError(val message: String) extends TokenError(message)

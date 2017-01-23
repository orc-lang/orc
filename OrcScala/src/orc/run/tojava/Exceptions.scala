//
// Exceptions.scala -- Exceptions for use in the toJava runtime
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.tojava

/** Notify the enclosing code that this direct form Orc code has halted.
  */
class HaltException() extends RuntimeException() {
}

/** Notify the enclosing code that this direct form Orc code has halted due to an expected exception in the Java code.
  */
class ExceptionHaltException(e: Throwable) extends HaltException {
  initCause(e)
}

object HaltException {
  /** A singleton instance of HaltException to avoid allocation.
    */
  val SINGLETON = new HaltException()
  /* NOTE: Using a singleton is the "right thing" for performance,
   * however it makes the stacks wrong. You can change this to a def
   * to get the stacks right.
   */

  final def throwIt() = throw SINGLETON
}

/** Notify the enclosing code that this Orc code has been killed.
  *
  * This is thrown by checkLive() and caught in Trim implementations.
  */
final class KilledException extends RuntimeException

object KilledException {
  /** A singleton instance of KilledException to avoid allocation.
    */
  val SINGLETON = new KilledException
  /* NOTE: Using a singleton is the "right thing" for performance,
   * however it makes the stacks wrong. You can change this to a def
   * to get the stacks right.
   */
}

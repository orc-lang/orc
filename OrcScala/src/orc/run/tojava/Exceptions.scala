package orc.run.tojava

/** Notify the enclosing code that this direct form Orc code has halted.
  */
final class HaltException extends RuntimeException

object HaltException {
  /** A singleton instance of HaltException to avoid allocation.
    */
  val SINGLETON = new HaltException
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
}
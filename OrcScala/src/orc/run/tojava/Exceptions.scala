package orc.run.tojava

final class HaltException extends RuntimeException

object HaltException {
  val SINGLETON = new HaltException
}

final class KilledException extends RuntimeException

object KilledException {
  val SINGLETON = new KilledException
}
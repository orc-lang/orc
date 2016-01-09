package orc.run.tojava

import orc.Schedulable
import orc.run.Logger

/** @author amp
  */
final class FunctionSchedulable(f: () => Unit) extends Schedulable {
  override val nonblocking = true
  def run(): Unit = {
    f()
  }
}

abstract class ContextSchedulable(ctx: Context) extends Schedulable {
  override val nonblocking = true

  override def onSchedule() = {
    ctx.prepareSpawn()
  }
  override def onComplete() = {
    ctx.halt
  }
}

final class ContextSchedulableFunc(ctx: Context, f: () => Unit) extends ContextSchedulable(ctx) {
  def run(): Unit = {
    Logger.info(s"Starting ${f.getClass()}@${f.hashCode()}")
    // Catch kills and continue.
    try {
      f()
    } catch {
      case _: KilledException =>
        ()
    }
    Logger.info(s"Finished ${f.getClass()}@${f.hashCode()}")
  }
}

final class ContextSchedulableRunnable(ctx: Context, f: Runnable) extends ContextSchedulable(ctx) {
  def run(): Unit = {
    Logger.info(s"Starting $f")
    // Catch kills and continue.
    try {
      f.run()
    } catch {
      case _: KilledException =>
        ()
    }
    Logger.info(s"Finished $f")
  }
}

package orc.run.tojava

import orc.Schedulable

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
    f()
  }
}

final class ContextSchedulableRunnable(ctx: Context, f: Runnable) extends ContextSchedulable(ctx) {
  def run(): Unit = {
    f.run()
  }
}

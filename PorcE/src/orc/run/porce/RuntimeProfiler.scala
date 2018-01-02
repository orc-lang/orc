package orc.run.porce

import com.oracle.truffle.api.CompilerDirectives.{ TruffleBoundary, CompilationFinal }
import orc.run.RuntimeProfiler

object RuntimeProfilerWrapper {
  @CompilationFinal
  private val profileRuntime = RuntimeProfiler.profileRuntime

  @CompilationFinal
  final val CallDispatch = RuntimeProfiler.CallDispatch
  @CompilationFinal
  final val SiteImplementation = RuntimeProfiler.SiteImplementation
  @CompilationFinal
  final val JavaDispatch = RuntimeProfiler.JavaDispatch
  
  def regionToString(region: Long) = RuntimeProfiler.regionToString(region)

  @inline
  def traceEnter(region: Long): Unit = {
    if (profileRuntime) {
      Boundaries.traceEnter(region, -1)
    }
  }

  @inline
  def traceEnter(region: Long, id: Int): Unit = {
    if (profileRuntime) {
      Boundaries.traceEnter(region, id)
    }
  }

  @inline
  def traceExit(region: Long): Unit = {
    if (profileRuntime) {
      Boundaries.traceExit(region, -1)
    }
  }

  @inline
  def traceExit(region: Long, id: Int): Unit = {
    if (profileRuntime) {
      Boundaries.traceExit(region, id)
    }
  }
  
  object Boundaries {
    @TruffleBoundary(allowInlining = true) @noinline
    def traceEnter(region: Long, id: Int): Unit = {
      RuntimeProfiler.traceEnter(region, id)
    }
    
    @TruffleBoundary(allowInlining = true) @noinline
    def traceExit(region: Long, id: Int): Unit = {
      RuntimeProfiler.traceExit(region, id)
    }
  }
}
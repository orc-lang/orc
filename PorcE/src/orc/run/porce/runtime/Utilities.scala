package orc.run.porce.runtime

import com.oracle.truffle.api.nodes.RootNode
import com.oracle.truffle.api.Truffle
import java.util.Arrays

object Utilities {
  private val emptyArray = Array[AnyRef]()
  
  def PorcEClosure(r: RootNode): PorcEClosure = {
    val c = Truffle.getRuntime().createCallTarget(r)
    orc.run.porce.runtime.PorcEClosure.create(emptyArray, c, false, null) 
  }
  
  def isDef(v: AnyRef): Boolean = v match {
    case c: PorcEClosure => {
      c.isRoutine
    }
    // TODO: Add support for external defs if we every have them supported in the API.
    case _ =>
      false
  }
}
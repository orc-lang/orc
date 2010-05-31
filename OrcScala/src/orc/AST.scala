package orc

import scala.util.parsing.input.Positional


abstract class AST extends Positional {

  def ->[B <: AST](f: this.type => B): B = {
      val location = this.pos
      val result = f(this)
      result.pos = location
      result
  }

  def !!(exn : Throwable with Positional) = {
      exn.pos = this.pos
      throw exn
  }
}

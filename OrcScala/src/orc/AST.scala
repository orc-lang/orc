package orc

import scala.util.parsing.input.Positional


class PositionalException(msg: String) extends Exception(msg) with Positional

abstract class AST extends Positional {

  def ->[B <: AST](f: this.type => B): B = {
      val location = this.pos
      val result = f(this)
      result.pos = location
      result
  }
  
  def ->>[B <: AST](that : B): B = { that.pos = this.pos ; that }

  def !!(exn : PositionalException): Nothing = {
      exn.pos = this.pos
      throw exn
  }
  
  
  // Remove this overloading to uncover uses of !! that do not carry a specific exception type
  def !!(msg : String): Nothing = !!(new PositionalException(msg))
}

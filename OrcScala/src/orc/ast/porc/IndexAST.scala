package orc.ast.porc

import orc.ast.ASTWithIndex

object IndexAST {
  def apply(ast: PorcAST): Unit = {
    var i = 0
    def nextIndex(): Int = {
      val r = i
      i += 1
      r
    }
    
    def process(ast: PorcAST): Unit = {
      ast match {
        case a: ASTWithIndex =>
          a.optionalIndex = Some(nextIndex())
        case _ =>
          ()
      }
      ast.subtrees.foreach(process)
    }
    
    process(ast)
  }
}
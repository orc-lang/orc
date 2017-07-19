package orc.ast.porc

import orc.compile.CompilerOptions
import scala.collection.mutable
import orc.error.compiletime.InternalCompilerError
import orc.error.compiletime.InternalCompilerWarning

object VariableChecker {
  def apply(e: PorcAST.Z, co: CompilerOptions): Unit = {
    checker(co)(e)
  }

  private def checkError(b: Boolean, msg: => String, e: PorcAST.Z)(implicit co: CompilerOptions): Unit = if (!b) {
    val exc = new InternalCompilerError(msg)
    e.value.sourceTextRange foreach { exc.setPosition(_) }
    co.reportProblem(exc)
  }

  private def checkWarning(b: Boolean, msg: => String, e: PorcAST.Z)(implicit co: CompilerOptions): Unit = if (!b) {
    val exc = new InternalCompilerWarning(msg)
    e.value.sourceTextRange foreach { exc.setPosition(_) }
    co.reportProblem(exc)
  }

  private def checker(implicit co: CompilerOptions) = new Transform {
    val encounteredVariables = mutable.HashSet[Variable]()

    override val onExpression: PartialFunction[Expression.Z, Expression] = {
      case e if e.value.boundVars.nonEmpty =>
        e.value.boundVars foreach { v =>
          checkError(!encounteredVariables.contains(v), s"Variable $v has already been declared in the program.", e)
          encounteredVariables += v
        }
        e.value
      case a: Argument.Z if onArgument.isDefinedAt(a) =>
        onArgument(a)
    }

    override val onMethod: PartialFunction[Method.Z, Method] = {
      case e if e.value.boundVars.nonEmpty =>
        e.value.boundVars foreach { v =>
          checkError(!encounteredVariables.contains(v), s"Variable $v has already been declared in the program.", e)
          encounteredVariables += v
        }
        e.value
    }

    override val onArgument: PartialFunction[Argument.Z, Argument] = {
      case v: Variable.Z =>
        checkError(v.contextBoundVars.contains(v.value), s"Variable is not declared in scope: $v", v)
        v.value
    }
  }
}
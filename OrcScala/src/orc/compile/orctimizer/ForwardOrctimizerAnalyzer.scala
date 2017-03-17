package orc.compile.orctimizer

import orc.compile.flowanalysis.AnalyzerWithAutoOutputs
import orc.compile.flowanalysis.Analyzer
import orc.ast.orctimizer.named._

sealed abstract class OrctimizerAnalyzerNode

case class ExpressionNode(expr: Expression) extends OrctimizerAnalyzerNode
case class VariableNode(v: BoundVar, binding: Expression) extends OrctimizerAnalyzerNode

abstract class ForwardOrctimizerAnalyzer(val root: Expression) extends Analyzer with AnalyzerWithAutoOutputs {
  type NodeT = OrctimizerAnalyzerNode

  val (inputsMap: collection.Map[NodeT, collection.Set[NodeT]], initialNodes: collection.Set[NodeT]) = {
    import scala.collection.mutable
    val map = mutable.HashMap[NodeT, Set[NodeT]]()
    val leaves = mutable.HashSet[NodeT]()

    def process(e: NamedAST): Unit = {
      e match {
        case e: Expression =>
          val ss = e.subtrees
          //Needs context so I can correctly select the binding for VariableNodes
          map += ((ExpressionNode(e), ss.collect({ case e: Expression => ExpressionNode(e) }).toSet))
          if (ss.isEmpty) {
            leaves += ExpressionNode(e)
          }
          ss.foreach(process)
        case _ => {}
      }
    }

    process(root)

    (map, leaves)
  }
}

/*

A single graph will not be enough for everything. For instance, any kind of "happens before" analysis
will need a control flow graph including edges representing things like the LHS of branch must publish
before the RHS. A universal graph is not practical.

The formal universally informative graph would have edges representing almost every element of the
semantics. Both data flow and execution flow.

All data flow edges represent some kind of control flow as well. But the kind of control flow varies.
Also not all control flow edges also carry data (for instance, otherwise does not).

The approach I took above took data flow edges very literally. Edges from a binding ran to the reference
to that variable and all information was represented as information about the output of each expression.
In normal DFA the information is pushed forward instead. Following the control flow and moving information
along as mappings from variables to the analysis results. That will not work naively with Orc since
a bound variable does not carry all the important information about an expression (since variable binding
is semantically complicated).

I should look into effect analysis since that has similar problems in that the important thing is not the
result but instead the effects of executing. Similarly I should look at PDGs.

I have a picture of a drawing from John and my talk about this.

*/

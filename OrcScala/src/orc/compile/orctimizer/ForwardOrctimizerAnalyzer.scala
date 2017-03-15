package orc.compile.orctimizer

import orc.compile.flowanalysis.AnalyzerWithAutoOutputs
import orc.compile.flowanalysis.Analyzer
import orc.ast.orctimizer.named._

abstract class ForwardOrctimizerAnalyzer(val root: Expression) extends Analyzer with AnalyzerWithAutoOutputs {
  type NodeT = Expression

  val (inputsMap: collection.Map[NodeT, collection.Set[NodeT]], initialNodes: collection.Set[NodeT]) = {
    import scala.collection.mutable
    val map = mutable.HashMap[NodeT, Set[NodeT]]()
    val leaves = mutable.HashSet[NodeT]()

    def process(e: NamedAST): Unit = {
      e match {
        case e: Expression =>
          val ss = e.subtrees
          map += ((e, ss.collect({ case e: Expression => e }).toSet))
          if (ss.isEmpty) {
            leaves += e
          }
          ss.foreach(process)
        case _ => {}
      }
    }

    process(root)

    (map, leaves)
  }
}

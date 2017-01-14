package orc.compile

import scala.collection.mutable

trait NamedOptimization {
  val name: String
}

trait OptimizerStatistics {
  private val _optimizationCounts = mutable.Map[String, Long]().withDefaultValue(0)
  
  def optimizationCounts: collection.Map[String, Long] = _optimizationCounts
  
  def countOptimization(s: String, n: Long = 1): Unit = {
    _optimizationCounts(s) += n
  }
  def countOptimization(o: NamedOptimization): Unit = {
    countOptimization(o.name, 1)
  }
  def countOptimization(o: NamedOptimization, n: Long): Unit = {
    countOptimization(o.name, n)
  }
}
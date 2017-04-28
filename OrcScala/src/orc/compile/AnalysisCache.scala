package orc.compile

class AnalysisCache {
  // TODO: Replace with LRU cache.
  val cache = collection.mutable.HashMap[(AnalysisRunner[_, _], Any), Any]()

  def get[T, P](runner: AnalysisRunner[P, T])(params: P): T = {
    cache.getOrElseUpdate((runner, params), runner.compute(this)(params)).asInstanceOf[T]
  }

  def clean(): Unit = {

  }
}

trait AnalysisRunner[P, T] {
  def compute(cache: AnalysisCache)(params: P): T
}

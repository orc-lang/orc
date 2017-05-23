package orc.compile.flowanalysis

trait LatticeValue[T <: LatticeValue[T]] {
  def combine(o: T): T
  def lessThan(o: T): Boolean
}

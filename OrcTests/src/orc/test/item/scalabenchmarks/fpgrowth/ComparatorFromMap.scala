package orc.test.item.scalabenchmarks.fpgrowth

import java.util.Comparator
import java.util.concurrent.atomic.LongAdder

object ComparatorFromMap {
  def apply[T <: Comparable[T]](m: java.util.Map[T, LongAdder]): Comparator[T] = {
    def extractSum(x: T) = Option(m.get(x)).map(_.sum()).getOrElse(Long.MinValue)
    //def extractHash(x: T) = Option(m.get(x)).map(_.##).getOrElse(Int.MinValue)
    
    (x, y) => {
      val b = extractSum(y) compare extractSum(x)
      if (b == 0) {
        x compareTo y
      } else b
    }
  }
}
package orc.util

object ArrayExtensions {
  object ArrayN {
    /** Called in a pattern match like `{ case Array(x,y,z) => println('3 elements')}`.
      *
      * @param x the selector value
      * @return  sequence wrapped in a [[scala.Some]], if `x` is a Seq, otherwise `None`
      */
    def unapplySeq[T](x: Array[T]): Option[IndexedSeq[T]] =
      if (x == null) None else Some(x)
  }

  object Array0 {
    def unapply[T](x: Array[T]): Boolean = {
      if (x != null && x.length == 0)
        true
      else
        false
    }
  }

  object Array1 {
    def unapply[T](x: Array[T]): Option[T] = {
      if (x != null && x.length == 1)
        Some(x(0))
      else
        None
    }
  }

  object Array2 {
    def unapply[T](x: Array[T]): Option[(T, T)] = {
      if (x != null && x.length == 2)
        Some((x(0), x(1)))
      else
        None
    }
  }

  object Array3 {
    def unapply[T](x: Array[T]): Option[(T, T, T)] = {
      if (x != null && x.length == 1)
        Some((x(0), x(1), x(2)))
      else
        None
    }
  }
}

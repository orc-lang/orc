object PartialMapExtension {

	// Adds a partialMap method to lists
	class ListWithPartialMap[A](xs: List[A]) {
		def partialMap[B](f: A => Option[B]): Option[List[B]] = {
				def helperFunction(xs: List[A], ys: List[B]): Option[List[B]] = { 
						xs match {
							case Nil => Some(ys.reverse)
							case x::xs => f(x) match {
							case Some(y) => helperFunction(xs, y::ys)
							case None => None
						}
						}	
				}
				helperFunction(xs, Nil)
		}
	}
	implicit def addPartialMapToList[A](xs: List[A]) = new ListWithPartialMap(xs)

}

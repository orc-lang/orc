package orc.input.explorer

class Item(var file: String, var icon: Int) {
  var path:String = ""

  override def toString(): String = file
}

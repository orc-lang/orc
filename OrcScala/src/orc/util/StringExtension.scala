package orc.util

object StringExtension {
  implicit class StringOperations(val s: String) extends AnyVal {
    def truncateTo(i: Int, marker: String = "[...]") = {
      assume(i > marker.length)
      if (s.length > i) {
        s.substring(0, i - marker.length) + marker
      } else {
        s
      }
    }
  }
}
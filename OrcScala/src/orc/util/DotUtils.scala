package orc.util

object DotUtils {
  def shortString(o: AnyRef) = s"'${o.toString().replace('\n', ' ').take(30)}'"
  def quote(s: String) = s.replace('"', '\'')

  type DotAttributes = Map[String, String]
  
  trait WithDotAttributes {
    def dotAttributes: DotAttributes

    def dotAttributeString = {
      s"[${dotAttributes.map(p => s"${p._1}=${'"'}${quote(p._2)}${'"'}").mkString(",")}]"
    }
  }
}

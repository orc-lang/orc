package orc.values

object NumericsConfig {
  @inline
  val preferLP = System.getProperty("orc.numerics.preferLP", "false").toBoolean  
  @inline
  val preferDouble = Option(System.getProperty("orc.numerics.preferDouble")).map(_.toBoolean).getOrElse(preferLP)  
  @inline
  val preferLong = Option(System.getProperty("orc.numerics.preferLong")).map(_.toBoolean).getOrElse(preferLP)
}
package orc.values

import scala.math.BigDecimal

object NumericsConfig {
  @inline
  final def toOrcIntegral(v: Number) = {
    if (NumericsConfig.preferLong) 
      v.longValue()
    else {
      v match {
        case d: BigInt =>
          d
        case d: BigDecimal =>
          d.toBigInt()
        case d: java.lang.Double =>
          BigDecimal(d).toBigInt()
        case _ if v.longValue() == v =>
          BigInt(v.longValue())
        case _ =>
          BigInt(v.toString)
      }
      if (v.longValue() == v) {
        BigInt(v.longValue())
      } else {
        BigInt(v.toString)
      }
    }
  }
  @inline
  final def toOrcFloatingPoint(v: Number) = {
    if (NumericsConfig.preferDouble) 
      v.doubleValue()
    else {
      v match {
        case d: BigInt =>
          BigDecimal(d)
        case d: BigDecimal =>
          d
        case d: java.lang.Double =>
          BigDecimal(d)
        case _ if v.doubleValue() == v =>
          BigDecimal(v.doubleValue())
        case _ =>
          BigDecimal(v.toString)
      }
      if (v.longValue() == v) {
        BigInt(v.longValue())
      } else {
        BigInt(v.toString)
      }
    }
  }
  
  @inline
  final val preferLP = System.getProperty("orc.numerics.preferLP", "false").toBoolean  
  @inline
  final val preferDouble = Option(System.getProperty("orc.numerics.preferDouble")).map(_.toBoolean).getOrElse(preferLP)  
  @inline
  final val preferLong = Option(System.getProperty("orc.numerics.preferLong")).map(_.toBoolean).getOrElse(preferLP)
}
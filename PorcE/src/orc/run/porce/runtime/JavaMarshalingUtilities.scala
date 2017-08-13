package orc.run.porce.runtime

object JavaMarshalingUtilities {
  def existsMarshalValueWouldReplace(values: Array[AnyRef], untypedMarshalValueWouldReplace: AnyRef => AnyRef): Boolean = {
    val marshalValueWouldReplace = untypedMarshalValueWouldReplace.asInstanceOf[AnyRef => Boolean]
    values exists marshalValueWouldReplace
  }
  def mapMarshaler(values: Array[AnyRef], marshaler: AnyRef => AnyRef): Array[AnyRef] = {
    values map marshaler
  }
}
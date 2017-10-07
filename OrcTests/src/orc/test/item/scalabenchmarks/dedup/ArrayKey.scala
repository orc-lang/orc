package orc.test.item.scalabenchmarks.dedup

import java.util.Arrays

class ArrayKey(val array: Array[Byte]) {
  override def hashCode() = Arrays.hashCode(array)
  override def equals(o: Any): Boolean = o match {
    case o: ArrayKey => Arrays.equals(array, o.array)
    case _ => false
  }
  
  override def toString(): String = {
    array.view.map(b => (b.toInt & 0xFF).formatted("%02x")).mkString("")
  }
}
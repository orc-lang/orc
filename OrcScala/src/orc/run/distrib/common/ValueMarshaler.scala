//
// ValueMarshaler.scala -- Scala traits ValueMarshaler and MashalingAndRemoteRefSupport
// Project OrcScala
//
// Created by jthywiss on Mar 3, 2017.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.common

import scala.collection.mutable.WeakHashMap

import orc.run.distrib.{ DOrcMarshalingNotifications, DOrcMarshalingReplacement, Logger }

trait MashalingAndRemoteRefSupport[Location] extends RemoteObjectManager[Location] with RemoteRefIdManager[Location] with ExecutionMarshaling[Location] with FollowerNumLocationMapping[Location] {
  def permittedLocations(v: Any): Set[Location]
}

/** A DOrcExecution mix-in to marshal and unmarshal Orc program values.
  *
  * When marshaling, if the ValueLocator permits the destination, and the
  * value is Serializable, then the object is OK as-is.  Otherwise, a
  * RemoteObjectRef is used.
  *
  * When unmarshaling, if the value is a RemoteObjectRef that is actually
  * local, the local object is substituted. Otherwise, the RemoteObjectRef is
  * used.
  *
  * @author jthywiss
  */
trait ValueMarshaler[Execution <: MashalingAndRemoteRefSupport[Location], Location] {
  execution: Execution =>

  def marshalValueWouldReplace(destination: Location)(value: Any): Boolean = {
    value match {
      /* keep in sync with cases in marshalValue */
      //FIXME: Handle circular references in DOrcMarshalingReplacement calls
      case dmr: DOrcMarshalingReplacement if dmr.isReplacementNeededForMarshaling(marshalValueWouldReplace(destination)(_)) => true
      case st: scala.collection.Traversable[Any] if st.exists(marshalValueWouldReplace(destination)(_)) => true //FIXME:Scala-specific, generalize
      case null => false
      case xo: AnyRef if execution.marshalExecutionObject.isDefinedAt((destination, xo)) => /* ExecutionMashaler handles, instead of ValueMarshaler */ false
      case ro: RemoteObjectRef => true
      case v: java.io.Serializable if execution.permittedLocations(v).contains(destination) && canReallySerialize(destination)(v) => false
      case _ /* Cannot be marshaled to this destination */ => true
    }
  }

  def marshalValue(destination: Location)(value: AnyRef): AnyRef = {
    //Logger.Marshal.finest(s"marshalValue: $value: ${orc.util.GetScalaTypeName(value)}.isInstanceOf[java.io.Serializable]=${value.isInstanceOf[java.io.Serializable]}")

    val replacedValue = value match {
      /* keep in sync with cases in marshalValueWouldReplace */
      //FIXME: Handle circular references in DOrcMarshalingReplacement calls
      case dmr: DOrcMarshalingReplacement if dmr.isReplacementNeededForMarshaling(marshalValueWouldReplace(destination)(_)) => dmr.replaceForMarshaling(marshalValue(destination)(_))
      case st: scala.collection.Traversable[Any] => st.map(_ match { //FIXME:Scala-specific, generalize
        case o: AnyRef => marshalValue(destination)(o)
        case x => x
      })
      case v => v
    }
    val executionMashalerWillHandle = execution.marshalExecutionObject.isDefinedAt((destination, replacedValue))
    val marshaledValue = replacedValue match {
      /* keep in sync with cases in marshalValueWouldReplace */
      case null => null
      case xo if executionMashalerWillHandle => /* Leave for ExecutionMashaler to handle */ xo
      case ro: RemoteObjectRef => ro.marshal()
      case v: java.io.Serializable if execution.permittedLocations(v).contains(destination) && canReallySerialize(destination)(v) => v
      case v /* Cannot be marshaled to this destination */ => {
        new RemoteObjectRef(execution.remoteIdForObject(v)).marshal()
      }
    }
    value match {
      case mn: DOrcMarshalingNotifications => mn.marshaled()
      case _ => { /* Nothing to do */ }
    }
    Logger.Marshal.finest(s"marshalValue($destination)($value): ${orc.util.GetScalaTypeName(value)} = $marshaledValue")
    //Logger.Marshal.finest(s"executionMashalerWillHandle = $executionMashalerWillHandle; (marshaledValue != value) = ${(marshaledValue != value)}; marshalValueWouldReplace(destination)(value) = ${marshalValueWouldReplace(destination)(value)}")

    assert(executionMashalerWillHandle || (marshaledValue != value) == marshalValueWouldReplace(destination)(value), s"marshaledValue disagrees with marshalValueWouldReplace for value=$value, marshaledValue=$marshaledValue")
    marshaledValue
  }

  private val knownGoodSerializables = new WeakHashMap[java.io.Serializable, Unit]()
  private val knownBadSerializables = new WeakHashMap[java.io.Serializable, Unit]()
  private val nullOos = new RuntimeConnectionOutputStream[Execution, Location, Execution#ExecutionId](new java.io.OutputStream { def write(b: Int): Unit = {} })
  /** Many, many objects violate the java.io.Serializable interface by
    * implementing Serializable, but then holding references to
    * non-Serializable values without using any of the compensating
    * mechanisms (transient, externalization, etc.).
    *
    * So we have to detect these broken objects before they cause I/O
    * exceptions during serialization.
    */
  private def canReallySerialize(destination: Location)(v: java.io.Serializable): Boolean = {
    if (isAlwaysSerializable(v))
      true
    else if (synchronized { knownGoodSerializables.contains(v) })
      true
    else if (synchronized { knownBadSerializables.contains(v) })
      false
    else {
      //FIXME: Terribly slow.  Leaks objects (refs in nullOos).  Just BadBadBad.
      /* Ideally, we'd do this via reflection, but the serialization logic is
       * intricate and complex enough that it's unlikely we'd re-implement it
       * consistently.
       *
       * Future work: Statically discover safe classes that only refer to
       * primitive or other safe classes.
       *
       * Future work: Build a "lint"-er that flags Serializable classes that
       * refer to un-serializable values.
       */
      try {
        nullOos synchronized {
          nullOos.setContext(execution, destination)
          try {
            nullOos.writeObject(v)
            nullOos.flush()
          } finally {
            nullOos.clearContext()
            nullOos.reset()
          }
        }
        synchronized { knownGoodSerializables.put(v, ()) }
        true
      } catch {
        case e: Exception => {
          synchronized { knownBadSerializables.put(v, ()) }
          Logger.Marshal.warning(s"Type ${orc.util.GetScalaTypeName(v)} is a LIAR: It implements java.io.Serializable, but throws a ${orc.util.GetScalaTypeName(e)} when written to an ObjectOutputStream. Instance='$v', Exception='$e'.")
          false
        }
      }
    }
  }

  private def isAlwaysSerializable(v: java.io.Serializable): Boolean =
    v == null || v.isInstanceOf[Class[_]] || v.isInstanceOf[String] || v.getClass.isPrimitive || v.getClass.isEnum ||
      (v.getClass.isArray && v.getClass.getComponentType.isPrimitive)

  def unmarshalValueWouldReplace(value: Any): Boolean = {
    value match {
      /* keep in sync with cases in unmarshalValue */
      case rrr: RemoteObjectRefReplacement => true
      //FIXME: Handle circular references in DOrcMarshalingReplacement calls
      case dmr: DOrcMarshalingReplacement if dmr.isReplacementNeededForUnmarshaling(unmarshalValueWouldReplace(_)) => true
      case st: scala.collection.Traversable[Any] if st.exists(unmarshalValueWouldReplace(_)) => true //FIXME:Scala-specific, generalize
      case _ => false
    }
  }

  def unmarshalValue(origin: Location)(value: AnyRef): AnyRef = {
    val unmarshaledValue = value match {
      /* keep in sync with cases in unmarshalValueWouldReplace */
      case rrr: RemoteObjectRefReplacement => rrr.unmarshal(execution)
      case _ => value
    }
    val replacedValue = unmarshaledValue match {
      /* keep in sync with cases in unmarshalValueWouldReplace */
      //FIXME: Handle circular references in DOrcMarshalingReplacement calls
      case dmr: DOrcMarshalingReplacement if dmr.isReplacementNeededForUnmarshaling(unmarshalValueWouldReplace(_)) => dmr.replaceForUnmarshaling(unmarshalValue(origin)(_))
      case st: scala.collection.Traversable[Any] => st.map(_ match { //FIXME:Scala-specific, generalize
        case o: AnyRef => unmarshalValue(origin)(o)
        case x => x
      })
      case v => v
    }
    replacedValue match {
      case mn: DOrcMarshalingNotifications => mn.unmarshaled()
      case _ => { /* Nothing to do */ }
    }
    Logger.Marshal.finest(s"unmarshalValue($value)=$replacedValue")
    assert((replacedValue != value) == unmarshalValueWouldReplace(value))
    replacedValue
  }

}

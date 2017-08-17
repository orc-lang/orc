//
// ValueMarshaler.scala -- Scala traits ValueMarshaler, DOrcMarshalingNotifications, and DOrcMarshalingReplacement
// Project PorcE
//
// Created by jthywiss on Mar 3, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.distrib

import scala.collection.mutable.WeakHashMap

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
trait ValueMarshaler {
  execution: DOrcExecution =>

  def marshalValueWouldReplace(destination: PeerLocation)(value: Any): Boolean = {
    value match {
      /* keep in sync with cases in marshalValue */
      //FIXME: Handle circular references
      case dmr: DOrcMarshalingReplacement if dmr.isReplacementNeededForMarshaling(marshalValueWouldReplace(destination)(_)) => true
      case st: scala.collection.Traversable[Any] if st.exists(marshalValueWouldReplace(destination)(_)) => true //FIXME:Scala-specific, generalize
      case null => false
      case ro: RemoteObjectRef => true
      case v: java.io.Serializable if execution.permittedLocations(v).contains(destination) && canReallySerialize(destination)(v) => false
      case _ /* Cannot be marshaled to this destination */ => true
    }
  }

  def marshalValue(destination: PeerLocation)(value: AnyRef): AnyRef = {
    //Logger.finest(s"marshalValue: $value:${value.getClass.getCanonicalName}.isInstanceOf[java.io.Serializable]=${value.isInstanceOf[java.io.Serializable]}")

    val replacedValue = value match {
      /* keep in sync with cases in marshalValueWouldReplace */
      //FIXME: Handle circular references
      case dmr: DOrcMarshalingReplacement if dmr.isReplacementNeededForMarshaling(marshalValueWouldReplace(destination)(_)) => dmr.replaceForMarshaling(marshalValue(destination)(_))
      case st: scala.collection.Traversable[Any] => st.map(_ match { //FIXME:Scala-specific, generalize
        case o: AnyRef => marshalValue(destination)(o)
        case x => x
      } )
      case v => v
    }
    val marshaledValue = replacedValue match {
      /* keep in sync with cases in marshalValueWouldReplace */
      case null => null
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
    //Logger.finest(s"marshalValue($destination)($value)=$marshaledValue")
    assert((marshaledValue != value) == marshalValueWouldReplace(destination)(value))
    marshaledValue
  }

  private val knownGoodSerializables = new WeakHashMap[java.io.Serializable, Unit]()
  private val knownBadSerializables = new WeakHashMap[java.io.Serializable, Unit]()
  private val nullOos = new RuntimeConnectionOutputStream(new java.io.OutputStream { def write(b: Int): Unit = {} })
  /** Many, many objects violate the java.io.Serializable interface by
    * implementing Serializable, but then holding references to
    * non-Serializable values without using any of the compensating
    * mechanisms (transient, externalization, etc.).
    *
    * So we have to detect these broken objects before they cause I/O
    * exceptions during serialization.
    */
  private def canReallySerialize(destination: PeerLocation)(v: java.io.Serializable): Boolean = {
    if (isAlwaysSerializable(v))
      true
    else if (knownGoodSerializables.contains(v))
      true
    else if (knownBadSerializables.contains(v))
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
          nullOos.writeObject(v)
          nullOos.flush()
          nullOos.clearContext()
        }
        knownGoodSerializables.put(v, ())
        true
      } catch {
        case e: Exception => {
          knownBadSerializables.put(v, ())
          Logger.warning(s"Type ${v.getClass.getTypeName} is a LIAR: It implements java.io.Serializable, but throws a ${e.getClass.getTypeName} when written to an ObjectOutputStream. Instance='$v', Exception='$e'.")
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
      //FIXME: Handle circular references
      case dmr: DOrcMarshalingReplacement if dmr.isReplacementNeededForUnmarshaling(unmarshalValueWouldReplace(_)) => true
      case st: scala.collection.Traversable[Any] if st.exists(unmarshalValueWouldReplace(_)) => true //FIXME:Scala-specific, generalize
      case _ => false
    }
  }

  def unmarshalValue(value: AnyRef): AnyRef = {
    val unmarshaledValue = value match {
      /* keep in sync with cases in unmarshalValueWouldReplace */
      case rrr: RemoteObjectRefReplacement => rrr.unmarshal(execution)
      case _ => value
    }
    val replacedValue = unmarshaledValue match {
      /* keep in sync with cases in unmarshalValueWouldReplace */
      //FIXME: Handle circular references
      case dmr: DOrcMarshalingReplacement if dmr.isReplacementNeededForUnmarshaling(unmarshalValueWouldReplace(_)) => dmr.replaceForUnmarshaling(unmarshalValue(_))
      case st: scala.collection.Traversable[Any] => st.map(_ match { //FIXME:Scala-specific, generalize
        case o: AnyRef => unmarshalValue(o)
        case x => x
      })
      case v => v
    }
    replacedValue match {
      case mn: DOrcMarshalingNotifications => mn.unmarshaled()
      case _ => { /* Nothing to do */ }
    }
    //Logger.finest(s"unmarshalValue($value)=$replacedValue")
    assert((replacedValue != value) == unmarshalValueWouldReplace(value))
    replacedValue
  }

}

/** Orc values implementing this trait will be notified of marshaling for
  * serialization to another location.
  *
  * @author jthywiss
  */
trait DOrcMarshalingNotifications {
  def marshaled(): Unit = {}
  def unmarshaled(): Unit = {}
}

/** Orc values implementing this trait will be asked for a marshalable
  * replacement for themselves when they are marshaled for serialization
  * to another location.
  *
  * @author jthywiss
  */
trait DOrcMarshalingReplacement {
  def isReplacementNeededForMarshaling(marshalValueWouldReplace: AnyRef => Boolean): Boolean
  def replaceForMarshaling(marshaler: AnyRef => AnyRef): AnyRef
  def isReplacementNeededForUnmarshaling(unmarshalValueWouldReplace: AnyRef => Boolean): Boolean
  def replaceForUnmarshaling(unmarshaler: AnyRef => AnyRef): AnyRef
}

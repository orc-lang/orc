//
// ValueMarshaler.scala -- Scala trait ValueMarshaler
// Project OrcScala
//
// Created by jthywiss on Mar 3, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib

/** A mix-in to marshal and unmarshal Orc program values.
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
trait ValueMarshaler { self: DOrcExecution =>

  def marshalValue(destination: PeerLocation)(value: AnyRef): AnyRef with java.io.Serializable = {
    //FIXME: Walk all fields and apply same policies.  Graph traversal and performance problems will manifest.
    //Logger.finest(s"marshalValue: $value:${value.getClass.getCanonicalName}.isInstanceOf[java.io.Serializable]=${value.isInstanceOf[java.io.Serializable]}")

    val mv = value match {
      case null => null
      case ro: RemoteObjectRef => ro.marshal()
      case v: java.io.Serializable if self.permittedLocations(v).contains(destination) && canReallySerialize(v) => v
      case v /* Cannot be marshaled to this destination */ => {
        new RemoteObjectRef(self.remoteIdForObject(v)).marshal()
      }
    }
    value match {
      case mn: DOrcMarshalingNotifications => mn.marshaled()
      case _ => { /* Nothing to do */ }
    }
    //Logger.finest(s"marshalValue($destination)($value)=$mv")
    mv
  }

  private val knownGoodSerializables = new java.util.WeakHashMap[java.io.Serializable, Unit]()
  private val knownBadSerializables = new java.util.WeakHashMap[java.io.Serializable, Unit]()
  private val nullOos = new java.io.ObjectOutputStream(new java.io.OutputStream { def write(b: Int) {} } )
  /** Many, many objects violate the java.io.Serializable interface by
    * implementing Serializable, but then holding references to 
    * non-Serializable values without using any of the compensating 
    * mechanisms (transient, externalization, etc.).
    * 
    * So we have to detect these broken objects before they cause I/O
    * exceptions during serialization.
    */
  private def canReallySerialize(v: java.io.Serializable): Boolean = {
    if (isAlwaysSerializable(v))
      true
    else if (knownGoodSerializables.containsKey(v))
      true
    else if (knownBadSerializables.containsKey(v))
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
        nullOos.writeObject(v)
        nullOos.flush()
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

  def unmarshalValue(value: AnyRef): AnyRef = {
    val v = value match {
      case rrr: RemoteObjectRefReplacement => rrr.unmarshal(self)
      case _ => value
    }
    v match {
      case mn: DOrcMarshalingNotifications => mn.unmarshaled()
      case _ => { /* Nothing to do */ }
    }
    //Logger.finest(s"unmarshalValue($value)=$v")
    v
  }
  
}

/** Orc values implementing this trait will be notified of marshaling for
  * serialization to another location.
  *
  * @author jthywiss
  */
trait DOrcMarshalingNotifications {
  def marshaled() {}
  def unmarshaled() {}
}

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
    val mv = value match {
      case null => null
      case ro: RemoteObjectRef => ro.marshal()
      case v: java.io.Serializable if self.permittedLocations(v).contains(destination) => v
      case v /* Cannot be marshaled to this destination */ => {
        new RemoteObjectRef(self.remoteIdForObject(v)).marshal()
      }
    }
    value match {
      case mn: DOrcMarshalingNotifications => mn.marshaled()
      case _ => { /* Nothing to do */ }
    }
    Logger.finest(s"marshalValue($destination)($value)=$mv")
    mv
  }

  def unmarshalValue(value: AnyRef): AnyRef = {
    val v = value match {
      case rrr: RemoteObjectRefReplacement => rrr.unmarshal(self)
      case _ => value
    }
    v match {
      case mn: DOrcMarshalingNotifications => mn.unmarshaled()
      case _ => { /* Nothing to do */ }
    }
    Logger.finest(s"unmarshalValue($value)=$v")
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

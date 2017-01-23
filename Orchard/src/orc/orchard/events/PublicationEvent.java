//
// PublicationEvent.java -- Java class PublicationEvent
// Project Orchard
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.events;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import scala.collection.JavaConversions;

import orc.values.Format;
import orc.values.OrcValue;

/**
 * Job publications (published Orc values).
 * 
 * @author quark
 */
@XmlSeeAlso({ PublicationEvent.ListMarshalProxy.class, PublicationEvent.OrcValueMarshalProxy.class, PublicationEvent.UnknownValueMarshalProxy.class })
public class PublicationEvent extends JobEvent {

    @XmlType(name = "list")
    public static class ListMarshalProxy {
        public Object[] elements;

        /** For JAXB */
        protected ListMarshalProxy() {
        }

        public ListMarshalProxy(final scala.collection.Iterable scalaList) {
            elements = new Object[scalaList.size()];
            int i = 0;
            for (final Object elem : JavaConversions.asJavaIterable(scalaList)) {
                elements[i++] = toXmlMarshallableValue(elem);
            }
        }

        @XmlAttribute
        public int getLength() {
            return elements.length;
        }
    }

    @XmlType(name = "orcValue")
    public static class OrcValueMarshalProxy {
        @XmlAttribute
        public String typeName;
        @XmlValue
        public String orcSyntax;

        /** For JAXB */
        protected OrcValueMarshalProxy() {
        }

        public OrcValueMarshalProxy(final String typeName, final String orcSyntax) {
            this.typeName = typeName;
            this.orcSyntax = orcSyntax;
        }

    }

    @XmlType(name = "otherValue")
    public static class UnknownValueMarshalProxy {
        @XmlAttribute
        public String typeName;
        @XmlAttribute
        public int hashCode;
        @XmlValue
        public String toString;

        /** For JAXB */
        protected UnknownValueMarshalProxy() {
        }

        public UnknownValueMarshalProxy(final String typeName, final int hashCode, final String toString) {
            this.typeName = typeName;
            this.hashCode = hashCode;
            this.toString = toString;
        }
    }

    @XmlTransient
    public Object value;

    public PublicationEvent() {
    }

    public PublicationEvent(final Object value) {
        this.value = value;
    }

    @XmlElement(name = "value", nillable = true, required = true)
    protected Object getXmlMarshallableValue() {
        return toXmlMarshallableValue(value);
    }

    protected static Object toXmlMarshallableValue(final Object value) {
        if (value == null
                ||
                // primitive types will be boxed if passed as args
                //value.getClass() == java.lang.Boolean.TYPE ||
                //value.getClass() == java.lang.Byte.TYPE ||
                //value.getClass() == java.lang.Short.TYPE ||
                //value.getClass() == java.lang.Integer.TYPE ||
                //value.getClass() == java.lang.Long.TYPE ||
                //value.getClass() == java.lang.Float.TYPE ||
                //value.getClass() == java.lang.Double.TYPE ||
                value instanceof java.lang.String || value instanceof java.lang.Character || value instanceof java.util.Calendar || value instanceof java.util.GregorianCalendar || value instanceof java.util.Date || value instanceof java.io.File || value instanceof java.net.URL || value instanceof java.net.URI || value instanceof java.lang.Class || value instanceof java.awt.Image || value instanceof javax.activation.DataHandler || value instanceof javax.xml.transform.Source
                || value instanceof javax.xml.datatype.XMLGregorianCalendar || value instanceof java.lang.Boolean || value instanceof byte[] || value instanceof java.lang.Byte || value instanceof java.lang.Short || value instanceof java.lang.Integer || value instanceof java.lang.Long || value instanceof java.lang.Float || value instanceof java.lang.Double || value instanceof java.math.BigInteger || value instanceof java.math.BigDecimal || value instanceof javax.xml.namespace.QName
                || value instanceof javax.xml.datatype.Duration || value instanceof java.lang.Void || value instanceof java.util.UUID) {
            return value;
        } else if (value instanceof scala.math.BigDecimal) {
            return ((scala.math.BigDecimal) value).bigDecimal();
        } else if (value instanceof scala.math.BigInt) {
            return ((scala.math.BigInt) value).bigInteger();
        } else if (value instanceof OrcValue) {
            return new OrcValueMarshalProxy(value.getClass().getCanonicalName(), ((OrcValue) value).toOrcSyntax());
        } else if (value instanceof orc.run.core.Closure) {
            //TODO:Is there any useful way to marshall this?
            final orc.run.core.Closure closure = (orc.run.core.Closure) value;
            return new UnknownValueMarshalProxy(value.getClass().getCanonicalName(), value.hashCode(), "{- " + closure.closureGroup().definitions().size() + " defs closed over " + closure.lexicalContext().size() + " bindings -}");
        } else if (value instanceof scala.Some) {
            return new OrcValueMarshalProxy(value.getClass().getCanonicalName(), "Some(" + Format.formatValueR(((scala.Some) value).x()) + ")");
        } else if (value instanceof scala.None) {
            return new OrcValueMarshalProxy(value.getClass().getCanonicalName(), "None()");
        } else if (value instanceof scala.collection.immutable.List) {
            return new ListMarshalProxy((scala.collection.immutable.List) value);
        } else {
            return new UnknownValueMarshalProxy(value.getClass().getCanonicalName(), value.hashCode(), value.toString());
        }
    }

    @Override
    public <E> E accept(final Visitor<E> visitor) {
        return visitor.visit(this);
    }
}

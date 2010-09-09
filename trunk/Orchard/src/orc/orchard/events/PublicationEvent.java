//
// PublicationEvent.java -- Java class PublicationEvent
// Project Orchard
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.events;

/**
 * Job publications (published Orc values).
 * @author quark
 */
public class PublicationEvent extends JobEvent {
	public Object value;

	public PublicationEvent() {
	}

	public PublicationEvent(final Object value) {
		this.value = makeMarshallable(value);
	}

	/**
	 * @param value2
	 * @return
	 */
	private Object makeMarshallable(Object orcValue) {
		if (orcValue == null ||
			orcValue instanceof java.lang.Character ||
			orcValue instanceof java.util.Calendar ||
			orcValue instanceof java.util.GregorianCalendar ||
			orcValue instanceof java.util.Date ||
			orcValue instanceof java.io.File ||
			orcValue instanceof java.net.URL ||
			orcValue instanceof java.net.URI ||
			orcValue instanceof java.lang.Class ||
			orcValue instanceof java.awt.Image ||
			orcValue instanceof javax.activation.DataHandler ||
			orcValue instanceof javax.xml.transform.Source ||
			orcValue instanceof javax.xml.datatype.XMLGregorianCalendar ||
			orcValue instanceof java.lang.Boolean ||
			orcValue instanceof byte[] ||
			orcValue instanceof java.lang.Byte ||
			orcValue instanceof java.lang.Short ||
			orcValue instanceof java.lang.Integer ||
			orcValue instanceof java.lang.Long ||
			orcValue instanceof java.lang.Float ||
			orcValue instanceof java.lang.Double ||
			orcValue instanceof java.math.BigInteger ||
			orcValue instanceof java.math.BigDecimal ||
			orcValue instanceof javax.xml.namespace.QName ||
			orcValue instanceof javax.xml.datatype.Duration ||
			orcValue instanceof java.lang.Void ||
			orcValue instanceof java.util.UUID) {
			return orcValue;
		} else if (orcValue instanceof scala.math.BigDecimal) {
			return ((scala.math.BigDecimal) orcValue).bigDecimal();
		} else if (orcValue instanceof scala.math.BigInt) {
			return ((scala.math.BigInt) orcValue).bigInteger();
		} else if (orcValue instanceof orc.values.Field) {
			//FIXME:Properly marshall this
			return orcValue.toString();
		} else if (orcValue instanceof orc.values.Signal$) {
			//FIXME:Properly marshall this
			return orcValue.toString();
		} else if (orcValue instanceof orc.values.TaggedValue) {
			//FIXME:Properly marshall this
			return orcValue.toString();
		} else if (orcValue instanceof orc.values.sites.Site) {
			//FIXME:Properly marshall this
			return orcValue.toString();
		//} else if (orcValue instanceof orc.run.Closure) {
		//	//FIXME:Properly marshall this
		//	return orcValue.toString();
		} else if (orcValue instanceof orc.values.OrcTuple) {
			//FIXME:Properly marshall this
			return orcValue.toString();
		} else if (orcValue instanceof scala.Some) {
			//FIXME:Properly marshall this
			return orcValue.toString();
		} else if (orcValue instanceof scala.None) {
			//FIXME:Properly marshall this
			return orcValue.toString();
		} else if (orcValue instanceof scala.collection.immutable.List) {
			//FIXME:Properly marshall this
			return orcValue.toString();
		} else if (orcValue instanceof orc.values.OrcRecord) {
			//FIXME:Properly marshall this
			return orcValue.toString();
		} else {
			//FIXME:Properly marshall this
			return orcValue.toString();
		}
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}

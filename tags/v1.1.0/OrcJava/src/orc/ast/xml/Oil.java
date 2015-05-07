//
// Oil.java -- Java class Oil
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.xml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.Writer;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import orc.Config;
import orc.ast.xml.expression.Expression;
import orc.error.compiletime.CompilationException;

public class Oil implements Serializable {
	@XmlAttribute(required = true)
	public String version;
	@XmlElement(required = true)
	public Expression expression;

	public Oil() {
	}

	public Oil(final orc.ast.oil.expression.Expression expression) throws CompilationException {
		this("1.0", expression.marshal());
	}

	public Oil(final String version, final Expression expression) {
		this.version = version;
		this.expression = expression;
	}

	@Override
	public String toString() {
		return super.toString() + "(" + version + ", " + expression + ")";
	}

	public orc.ast.oil.expression.Expression unmarshal(final Config config) throws CompilationException {
		return expression.unmarshal(config);
	}

	public String toXML() {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		JAXB.marshal(this, out);
		return out.toString();
	}

	public void toXML(final Writer out) {
		JAXB.marshal(this, out);
	}

	public static Oil fromXML(final String xml) {
		final StringReader in = new StringReader(xml);
		return JAXB.unmarshal(in, Oil.class);
	}

	public static Oil fromXML(final Reader in) {
		return JAXB.unmarshal(in, Oil.class);
	}

	/**
	 * Generate the schema definition.
	 * @throws JAXBException 
	 * @throws IOException 
	 */
	public static void main(final String[] args) throws JAXBException, IOException {
		final File baseDir = new File(".");
		class MySchemaOutputResolver extends SchemaOutputResolver {
			@Override
			public Result createOutput(final String namespaceUri, final String suggestedFileName) throws IOException {
				final File file = new File(baseDir, suggestedFileName);
				System.out.println("Writing to " + file);
				return new StreamResult(file);
			}
		}

		final JAXBContext context = JAXBContext.newInstance(Oil.class);
		context.generateSchema(new MySchemaOutputResolver());
	}
}

package orc.ast.oil.xml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import orc.Config;
import orc.Orc;
import orc.env.Env;
import orc.error.compiletime.CompilationException;

public class Oil implements Serializable {
	@XmlAttribute(required=true)
	public String version;
	@XmlElement(required=true)
	public Expression expression;
	public Oil() {}
	
	public Oil(orc.ast.oil.Expr expression) {
		this("1.0", expression.accept(new Marshaller()));
	}
	public Oil(String version, Expression expression) {
		this.version = version;
		this.expression = expression;
	}
	public String toString() {
		return super.toString() + "(" + version + ", " + expression + ")";
	}
	public orc.ast.oil.Expr unmarshal() {
		return expression.unmarshal();
	}
	public String toXML() {
		ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		JAXB.marshal(this, out);
		return out.toString();
	}
	public static Oil fromXML(String xml) {
		StringReader in = new StringReader(xml);
		return JAXB.unmarshal(in, Oil.class);
	}
	
	/**
	 * Generate the schema definition.
	 * @throws JAXBException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws JAXBException, IOException {
		final File baseDir = new File(".");
		class MySchemaOutputResolver extends SchemaOutputResolver {
		    public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
		    	File file = new File(baseDir, suggestedFileName);
		    	System.out.println("Writing to " + file);
		        return new StreamResult(file);
		    }
		}

		JAXBContext context = JAXBContext.newInstance(Oil.class);
		context.generateSchema(new MySchemaOutputResolver());
	}
}
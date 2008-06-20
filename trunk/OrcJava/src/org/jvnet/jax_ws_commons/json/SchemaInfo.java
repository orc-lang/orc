package org.jvnet.jax_ws_commons.json;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jws.soap.SOAPBinding.Style;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.ws.WebServiceException;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;
import org.codehaus.jettison.mapped.JaxwsMappedXMLStreamWriter;
import org.codehaus.jettison.mapped.MappedXMLStreamReader;
import org.jvnet.jax_ws_commons.json.schema.CompositeJsonType;
import org.jvnet.jax_ws_commons.json.schema.JsonOperation;
import org.jvnet.jax_ws_commons.json.schema.JsonTypeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.istack.NotNull;
import com.sun.xml.bind.unmarshaller.DOMScanner;
import com.sun.xml.stream.buffer.MutableXMLStreamBuffer;
import com.sun.xml.stream.buffer.stax.StreamWriterBufferCreator;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.server.DocumentAddressResolver;
import com.sun.xml.ws.api.server.SDDocument;
import com.sun.xml.ws.api.server.ServiceDefinition;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.model.wsdl.WSDLBoundPortTypeImpl;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSSchema;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.parser.JAXPParser;
import com.sun.xml.xsom.parser.XMLParser;
import com.sun.xml.xsom.parser.XSOMParser;
import com.sun.xml.xsom.visitor.XSVisitor;

/**
 * Captures the information parsed from XML Schema.
 * Used to guide the JSON/XML conversion.
 *
 * @author Kohsuke Kawaguchi
 */
public final class SchemaInfo {
    /**
     * Endpoint for which this schema info applies.
     */
    final @NotNull WSEndpoint endpoint;

    /**
     * Parent tag name to possible child tag names.
     */
    final Set<QName> tagNames = new HashSet<QName>();

    final List<JsonOperation> operations = new ArrayList<JsonOperation>();

    final SchemaConvention convention;


    /**
     * @throws WebServiceException
     *      If failed to parse schema portion inside WSDL.
     */
    public SchemaInfo(WSEndpoint endpoint) {
        this.endpoint = endpoint;

        final ServiceDefinition sd = endpoint.getServiceDefinition();
        final Map<String,SDDocument> byURL = new HashMap<String,SDDocument>();

        for (SDDocument doc : sd)
            byURL.put(doc.getURL().toExternalForm(),doc);

        // set up XSOMParser to read from SDDocuments
        XSOMParser p = new XSOMParser(new XMLParser() {
            private final XMLParser jaxp = new JAXPParser();

            public void parse(InputSource source, ContentHandler handler, ErrorHandler errorHandler, EntityResolver entityResolver) throws SAXException, IOException {
                SDDocument doc = byURL.get(source.getSystemId());
                if(doc!=null) {
                    try {
                        readToBuffer(doc).writeTo(handler,errorHandler,false);
                    } catch (XMLStreamException e) {
                        throw new SAXException(e);
                    }
                } else {
                    // default behavior
                    jaxp.parse(source,handler,errorHandler,entityResolver);
                }
            }
        });

        try {
            // parse the primary WSDL, and it should recursively parse all referenced schemas
            // TODO: this is super slow
            TransformerHandler h = ((SAXTransformerFactory) TransformerFactory.newInstance()).newTransformerHandler();
            DOMResult r = new DOMResult();
            h.setResult(r);
            readToBuffer(sd.getPrimary()).writeTo(h,false);
            Document dom = (Document)r.getNode();
            NodeList schemas = dom.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "schema");
            for( int i=0; i<schemas.getLength(); i++ ) {
                DOMScanner scanner = new DOMScanner();
                scanner.setContentHandler(p.getParserHandler());
                scanner.scan(schemas.item(i));
            }

            extractTagNames(p.getResult());
            convention = new SchemaConvention(tagNames);

            if(endpoint.getPort()!=null)
                buildJsonSchema(p.getResult(),endpoint.getPort());
        } catch (XMLStreamException e) {
            throw new WebServiceException("Failed to parse WSDL",e);
        } catch (IOException e) {
            throw new WebServiceException("Failed to parse WSDL",e);
        } catch (SAXException e) {
            throw new WebServiceException("Failed to parse WSDL",e);
        } catch (TransformerConfigurationException e) {
            throw new AssertionError(e); // impossible
        }
    }

    public String getServiceName() {
        String name = endpoint.getPort().getName().getLocalPart();
        if(name.endsWith("ServicePort"))
            // when doing java2wsdl and the class name ends with 'Service', you get this.
            name = name.substring(0,name.length()-4);
        return name;
    }

    public XMLStreamWriter createXMLStreamWriter(Writer writer) throws XMLStreamException {
        return new JaxwsMappedXMLStreamWriter(convention, writer);
    }

    public XMLStreamReader createXMLStreamReader(JSONTokener tokener) throws JSONException, XMLStreamException {
        return new MappedXMLStreamReader(new JSONObject(tokener), convention);
    }

    /**
     * Extracts parent/child tag name relationship.
     */
    private void extractTagNames(XSSchemaSet schemas) {
        XSVisitor collector = new SchemaWalker() {
            public void elementDecl(XSElementDecl decl) {
                tagNames.add(new QName(decl.getTargetNamespace(),decl.getName()));
            }
        };
        for( XSSchema s : schemas.getSchemas() )
            s.visit(collector);
    }

    private MutableXMLStreamBuffer readToBuffer(SDDocument doc) throws XMLStreamException, IOException {
        MutableXMLStreamBuffer buf = new MutableXMLStreamBuffer();
        doc.writeTo(null,resolver,new StreamWriterBufferCreator(buf));
        return buf;
    }

    private static final DocumentAddressResolver resolver = new DocumentAddressResolver() {
        public String getRelativeAddressFor(@NotNull SDDocument current, @NotNull SDDocument referenced) {
            return referenced.getURL().toExternalForm();
        }
    };

    private void buildJsonSchema(XSSchemaSet schemas, WSDLPort port) {
        Style style = ((WSDLBoundPortTypeImpl) port.getBinding()).getStyle();
        JsonTypeBuilder builder = new JsonTypeBuilder(convention);
        for( WSDLBoundOperation bo : port.getBinding().getBindingOperations() )
            operations.add(new JsonOperation(bo,schemas,builder,style));
    }

    public List<JsonOperation> getOperations() {
        return operations;
    }

    public Set<CompositeJsonType> getTypes() {
        Set<CompositeJsonType> r = new LinkedHashSet<CompositeJsonType>();
        for (JsonOperation op : operations) {
            op.input.listCompositeTypes(r);
            op.output.listCompositeTypes(r);
        }
        return r;
    }
    
    //private static final String WSDL_NSURI = "http://schemas.xmlsoap.org/wsdl/";
}

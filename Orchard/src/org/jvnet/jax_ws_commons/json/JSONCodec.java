package org.jvnet.jax_ws_commons.json;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Codec;
import com.sun.xml.ws.api.pipe.ContentType;
import com.sun.xml.ws.api.server.EndpointAwareCodec;
import com.sun.xml.ws.api.server.EndpointComponent;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.transport.http.HttpMetadataPublisher;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONTokener;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;


/**
 * Server-side {@link Codec} that generates JSON. 
 *
 * @author Jitendra Kotamraju
 */
class JSONCodec implements EndpointAwareCodec, EndpointComponent {

    private static final String JSON_MIME_TYPE = "application/json";
    private static final ContentType jsonContentType = new JSONContentType();

    private final WSBinding binding;
    private final SOAPVersion soapVersion;

    private SchemaInfo schemaInfo;
    private HttpMetadataPublisher metadataPublisher;
    private WSEndpoint endpoint;

    public JSONCodec(WSBinding binding) {
        this.binding = binding;
        this.soapVersion = binding.getSOAPVersion();
    }

    public JSONCodec(JSONCodec that) {
        this(that.binding);
        this.schemaInfo = that.schemaInfo;
        this.endpoint = that.endpoint;
    }

    public void setEndpoint(WSEndpoint endpoint) {
        this.endpoint = endpoint;
        schemaInfo = new SchemaInfo(endpoint);
        endpoint.getComponentRegistry().add(this);
    }

    public String getMimeType() {
        return JSON_MIME_TYPE;
    }

    public ContentType getStaticContentType(Packet packet) {
        return jsonContentType;
    }


    public @Nullable <T> T getSPI(@NotNull Class<T> type) {
        if(type==HttpMetadataPublisher.class) {
            if(metadataPublisher==null)
                metadataPublisher = new MetadataPublisherImpl(checkSchemaInfo());
            return type.cast(metadataPublisher);
        }
        return null;
    }

    public ContentType encode(Packet packet, OutputStream out) throws IOException {
        Message message = packet.getMessage();
        if (message != null) {
            XMLStreamWriter sw = null;
            try {
                sw = checkSchemaInfo().createXMLStreamWriter(new OutputStreamWriter(out,"UTF-8"));
                sw.writeStartDocument();
                message.writePayloadTo(sw);
                sw.writeEndDocument();
            } catch(XMLStreamException xe) {
                throw new WebServiceException(xe);
            } finally {
                if (sw != null) {
                    try {
                        sw.close();
                    } catch(XMLStreamException xe) {
                        // let the original exception get through
                    }
                }
            }
        }
        return jsonContentType;
    }

    public ContentType encode(Packet packet, WritableByteChannel buffer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the up-to-date {@link SchemaInfo} for the current endpoint,
     * by either using a cache or by parsing new.
     */
    private SchemaInfo checkSchemaInfo() {
        if(schemaInfo==null)
            throw new IllegalStateException("JSON binding is only available for the server");
        return schemaInfo;
    }

    public Codec copy() {
        return new JSONCodec(this);
    }

    public void decode(InputStream in, String contentType, Packet response) throws IOException {
        Message message;

        try {
            StringWriter sw = new StringWriter();
            // TODO: RFC-4627 calls for BOM check
            // TODO: honor charset sub header.
            Reader r = new InputStreamReader(in,"UTF-8");
            char[] buf = new char[1024];
            int len;
            while((len=r.read(buf))>=0)
                sw.write(buf,0,len);
            r.close();

            if(sw.getBuffer().length()==0) {
                // no content
                message = Messages.createEmpty(soapVersion);
            } else {
                XMLStreamReader reader = checkSchemaInfo().createXMLStreamReader(new JSONTokener(sw.toString()));
                message = Messages.createUsingPayload(reader, soapVersion);
            }
        } catch(XMLStreamException e) {
            throw new WebServiceException(e);
        } catch (JSONException e) {
            throw new WebServiceException(e);
        }

        response.setMessage(message);
    }

    public void decode(ReadableByteChannel in, String contentType, Packet response) {
        throw new UnsupportedOperationException();
    }

    private static final class  JSONContentType implements ContentType {

        private static final String JSON_CONTENT_TYPE = JSON_MIME_TYPE;

        public String getContentType() {
            return JSON_CONTENT_TYPE;
        }

        public String getSOAPActionHeader() {
            return null;
        }

        public String getAcceptHeader() {
            return JSON_CONTENT_TYPE;
        }

    }
}

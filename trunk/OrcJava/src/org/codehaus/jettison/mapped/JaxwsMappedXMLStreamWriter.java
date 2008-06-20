package org.codehaus.jettison.mapped;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.XMLStreamException;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sun.istack.XMLStreamException2;

/**
 * In order to override this class's methods we have to put the extension
 * into the same package.
 * @author quark
 */
public class JaxwsMappedXMLStreamWriter extends MappedXMLStreamWriter {
	public JaxwsMappedXMLStreamWriter(MappedNamespaceConvention arg0, Writer arg1) {
		super(arg0, arg1);
	}
	
	public void writeEndDocument() throws XMLStreamException {
        try {
            // unwrap the root
        	try {
        		root = root.getJSONObject((String)root.keys().next());
        	} catch (JSONException e) {
        		// ORC: bugfix to handle void methods
        		root = null;
        	}

            Object v;
            // if this is the sole return value unwrap that, too
            if (root != null && root.length()==1)
                v = root.get((String)root.keys().next());
            else
                v = root;

            // write
            if (v instanceof JSONObject) {
                ((JSONObject)v).write(writer);
            } else if (v instanceof JSONArray) {
                ((JSONArray)v).write(writer);
            } else if (v==null) {
                writer.write("null");
            } else if (v instanceof String) {
                writer.write('"'+v.toString()+'"');
            } else {
                writer.write(v.toString());
            }
            writer.flush();
        } catch (JSONException e) {
            throw new XMLStreamException2(e);
        } catch (IOException e) {
            throw new XMLStreamException2(e);
        }
    }
}

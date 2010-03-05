package org.jvnet.jax_ws_commons.json;

import com.sun.xml.ws.developer.JAXWSProperties;
import com.sun.xml.ws.model.wsdl.WSDLBoundOperationImpl;
import com.sun.xml.ws.transport.http.HttpAdapter;
import com.sun.xml.ws.transport.http.WSHTTPConnection;
import org.jvnet.jax_ws_commons.json.schema.JsonOperation;

import java.beans.Introspector;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Iterator;

/**
 * Generates javascript stub code that is used to access the endpoint.
 * 
 * @author Jitendra Kotamraju
 */
final class ClientGenerator {
    private final SchemaInfo model;
    private final WSHTTPConnection connection;
    private final HttpAdapter adapter;
    private String name;

    public ClientGenerator(SchemaInfo model, WSHTTPConnection connection, HttpAdapter adapter) {
        this.model = model;
        this.connection = connection;
        this.adapter = adapter;
        this.name = Introspector.decapitalize(model.getServiceName());
    }

    public void setVariableName(String name) {
        this.name = name;
    }

    void generate(PrintWriter os) throws IOException {
        writeGlobal(os);
        writeStatic(os);
        writeOperations(os);
        writeClosure(os);
        os.close();
    }

    private void writeGlobal(PrintWriter os) throws IOException {
    	if (functionName != null) {
    		os.printf("%s({\n", functionName);
    	} else {
    		os.printf("%s = {\n",name);
    	}
        shift(os);
        os.printf("url : \"%s\",\n", getRequestURL(connection, adapter));
    }

    private void writeStatic(PrintWriter os) throws IOException {
        Reader is = new InputStreamReader(getClass().getResourceAsStream("jaxws.js"));
        char[] buf = new char[256];
        int len;
        while((len = is.read(buf)) != -1) {
            os.write(buf,0,len);
        }
        is.close();
    }

    private void writeOperations(PrintWriter os) {
        Iterator<JsonOperation> it = model.operations.iterator();
        while(it.hasNext()) {
            writeOperation(it.next(), it.hasNext(), os);
        }
    }

    private void writeOperation(JsonOperation op, boolean next, PrintWriter os) {
        String reqName = model.convention.x2j.get(
            ((WSDLBoundOperationImpl)op.operation).getReqPayloadName());

        shift(os);
        os.printf("%s : function(obj, onResult, onError) {\n", op.methodName);
        shift2(os);
        os.printf("this.post({%s:obj}, onResult, onError);\n", reqName);
        shift(os);
        if (next) { os.append("},\n\n"); } else { os.append("}\n\n"); }
    }

    private static void shift(PrintWriter os) {
        os.append("    ");
    }

    private static void shift2(PrintWriter os) {
        shift(os);
        shift(os);
    }

    private void writeClosure(PrintWriter os) {
    	if (functionName != null) {
    		os.println("});");
    	} else {
    		os.println("};");
    	}
    }
    
    /**
     * ORC: call a function with the service when it's ready
     * rather than write to a variable.
     */
    public void setFunctionName(String func) {
    	this.functionName = func;
    }
    private String functionName;
    
    /**
     * ORC: fix bad request URI in servlets
     * @param con
     * @param adapter
     * @return
     */
    public static String getRequestURL(WSHTTPConnection con, HttpAdapter adapter) {
    	try {
    		return (String)con.get(JAXWSProperties.HTTP_REQUEST_URL);
    	} catch (IllegalArgumentException e) {
    		return con.getBaseAddress()+adapter.urlPattern;
    	}
    }
}

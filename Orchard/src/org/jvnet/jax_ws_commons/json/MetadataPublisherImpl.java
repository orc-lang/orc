package org.jvnet.jax_ws_commons.json;

import com.sun.istack.internal.NotNull;
import com.sun.xml.ws.transport.http.HttpAdapter;
import com.sun.xml.ws.transport.http.HttpMetadataPublisher;
import com.sun.xml.ws.transport.http.WSHTTPConnection;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

/**
 * Responds to "http://foobar/service?js" and sends the JavaScript proxy.
 *
 * @author Kohsuke Kawaguchi
 */
final class MetadataPublisherImpl extends HttpMetadataPublisher {
    private SchemaInfo model;

    public MetadataPublisherImpl(SchemaInfo model) {
        this.model = model;
    }

    @Override
    public boolean handleMetadataRequest(@NotNull HttpAdapter adapter, @NotNull WSHTTPConnection con) throws IOException {
        QueryStringParser qsp = new QueryStringParser(con);
        if(qsp.containsKey("js")) {
            // JavaScript proxy code
            con.setStatus(HttpURLConnection.HTTP_OK);
            con.setContentTypeResponseHeader("application/javascript;charset=utf-8");

            ClientGenerator gen = new ClientGenerator(model, con, adapter);
            String varName = qsp.get("var");
            if(varName!=null)
                gen.setVariableName(varName);
            // ORC: set function name to call with results
            String funcName = qsp.get("func");
            if(funcName!=null)
                gen.setFunctionName(funcName);

            gen.generate(new PrintWriter(
                new OutputStreamWriter(con.getOutput(),"UTF-8")));
            return true;
        }

        if(con.getQueryString()==null || qsp.containsKey("help")) {
            // index page
            con.setStatus(HttpURLConnection.HTTP_OK);
            con.setContentTypeResponseHeader("text/html;charset=UTF-8");

            generateHelpHtml(con,adapter,new OutputStreamWriter(con.getOutput(), "UTF-8"));
            return true;
        }

        URL res = getClass().getResource("template/" + con.getQueryString());
        if(res!=null) {
            // static resource accesss
            con.setStatus(HttpURLConnection.HTTP_OK);
            if(res.getPath().endsWith(".gif"))
                con.setContentTypeResponseHeader("image/gif");
            if(res.getPath().endsWith(".css"))
                con.setContentTypeResponseHeader("text/css");

            InputStream is = res.openStream();
            OutputStream os = con.getOutput();
            byte[] buf = new byte[1024];
            int len;
            while((len=is.read(buf))>=0)
                os.write(buf,0,len);
            is.close();
            os.close();
            return true;
        }

        return false;
    }

    /*package for testing*/ void generateHelpHtml(WSHTTPConnection con, HttpAdapter adapter, OutputStreamWriter writer) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("model",model);
        context.put("requestURL",ClientGenerator.getRequestURL(con, adapter));

        new VelocityEngine().evaluate(context, writer, "velocity",
            new InputStreamReader(getClass().getResourceAsStream("template/index.html"),"UTF-8")
            );
        writer.close();
    }
}

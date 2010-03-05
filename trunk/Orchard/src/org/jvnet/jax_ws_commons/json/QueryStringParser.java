package org.jvnet.jax_ws_commons.json;

import com.sun.xml.ws.transport.http.WSHTTPConnection;

import java.util.HashMap;

/**
 * Quick-n-dirty query string parser.
 *
 * @author Kohsuke Kawaguchi
 */
final class QueryStringParser extends HashMap<String,String> {
    QueryStringParser(WSHTTPConnection con) {
        this(con.getQueryString());
    }
    QueryStringParser(String queryString) {
        if(queryString==null)   return;

        for( String token : queryString.split("&") ) {
            int idx = token.indexOf('=');
            if(idx<0)
                put(token,"");
            else
                put(token.substring(0,idx),token.substring(idx+1));
        }
    }
}

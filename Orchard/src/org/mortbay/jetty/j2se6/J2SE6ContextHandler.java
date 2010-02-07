//========================================================================
//$$Id: J2SE6ContextHandler.java 550 2007-11-02 15:23:21Z lorban $$
//
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.jetty.j2se6;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.ContextHandler;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.Authenticator.Result;

/**
 * Jetty handler that bridges requests to {@link HttpHandler}.
 * @author lorban
 */
public class J2SE6ContextHandler extends ContextHandler
{

    private HttpContext _context;

    private HttpHandler _handler;

    public J2SE6ContextHandler(HttpContext context, HttpHandler handler)
    {
        this._context = context;
        this._handler = handler;
    }

    @Override
    public void handle(String target, HttpServletRequest req,
            HttpServletResponse resp, int dispatch) throws IOException,
            ServletException
    {
        if (!target.startsWith(getContextPath())) return;

        JettyHttpExchange jettyHttpExchange = new JettyHttpExchange(_context, req, resp);
        
        // TODO: add filters processing

        Authenticator auth = _context.getAuthenticator();
        try
        {
	        if (auth != null)
	            handleAuthentication(resp, jettyHttpExchange, auth);
	        else
	        	_handler.handle(jettyHttpExchange);
        }
        catch(Exception ex)
        {
        	PrintWriter writer = new PrintWriter(jettyHttpExchange.getResponseBody());
        	
        	resp.setStatus(500);
        	writer.println("<h2>HTTP ERROR: 500</h2>");
        	writer.println("<pre>INTERNAL_SERVER_ERROR</pre>");
        	writer.println("<p>RequestURI=" + req.getRequestURI() + "</p>");
        	
        	writer.println("<pre>");
			ex.printStackTrace(writer);
        	writer.println("</pre>");
        	
        	writer.println("<p><i><small><a href=\"http://jetty.mortbay.org\">Powered by jetty://</a></small></i></p>");
        	
        	writer.close();
        }
        finally
        {
			Request base_request = (req instanceof Request) ? (Request)req:HttpConnection.getCurrentConnection().getRequest();
			base_request.setHandled(true);
        }
        
    }

	private void handleAuthentication(HttpServletResponse resp, JettyHttpExchange jettyHttpExchange, Authenticator auth) throws IOException
	{
		Result result = auth.authenticate(jettyHttpExchange);
        if (result instanceof Authenticator.Failure)
        {
        	int rc = ((Authenticator.Failure)result).getResponseCode();
        	resp.sendError(rc);
        }
        else if (result instanceof Authenticator.Retry)
        {
        	int rc = ((Authenticator.Retry)result).getResponseCode();
        	resp.sendError(rc);
        }
        else if (result instanceof Authenticator.Success)
        {
        	HttpPrincipal p = ((Authenticator.Success)result).getPrincipal();
        	jettyHttpExchange.setPrincipal(p);
    		_handler.handle(jettyHttpExchange);
        }
	}

    public HttpHandler getHttpHandler()
    {
        return _handler;
    }

    public void setHttpHandler(HttpHandler handler)
    {
        this._handler = handler;
    }

}

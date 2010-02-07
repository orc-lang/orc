//========================================================================
//$$Id: JettyHttpContext.java 549 2007-11-02 12:41:46Z lorban $$
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Jetty implementation of {@link com.sun.net.httpserver.HttpContext}
 * @author lorban
 */
public class JettyHttpContext extends com.sun.net.httpserver.HttpContext
{

    private J2SE6ContextHandler _jettyContextHandler;

    private HttpServer server;
    
    private Map<String,Object> _attributes = new HashMap<String,Object>();
    
    private List<Filter> _filters = new ArrayList<Filter>();
    
    private Authenticator _authenticator;
    

    protected JettyHttpContext(HttpServer server, String path,
            HttpHandler handler)
    {
        this.server = server;
        _jettyContextHandler = new J2SE6ContextHandler(this, handler);
        _jettyContextHandler.setContextPath(path);
    }

    protected J2SE6ContextHandler getJettyContextHandler()
    {
        return _jettyContextHandler;
    }

    @Override
    public HttpHandler getHandler()
    {
        return _jettyContextHandler.getHttpHandler();
    }

    @Override
    public void setHandler(HttpHandler h)
    {
        _jettyContextHandler.setHttpHandler(h);
    }

    @Override
    public String getPath()
    {
        return _jettyContextHandler.getContextPath();
    }

    @Override
    public HttpServer getServer()
    {
        return server;
    }

    @Override
    public Map<String, Object> getAttributes()
    {
        return _attributes;
    }

    @Override
    public List<Filter> getFilters()
    {
        return _filters;
    }

    @Override
    public Authenticator setAuthenticator(Authenticator auth)
    {
    	Authenticator previous = _authenticator;
    	_authenticator = auth;
        return previous;
    }

    @Override
    public Authenticator getAuthenticator()
    {
        return _authenticator;
    }

}

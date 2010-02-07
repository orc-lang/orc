//========================================================================
//$$Id: JettyHttpServer.java 549 2007-11-02 12:41:46Z lorban $$
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
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.log.Log;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;

/**
 * Jetty implementation of {@link com.sun.net.httpserver.HttpServer}.
 * @author lorban
 */
public class JettyHttpServer extends com.sun.net.httpserver.HttpServer
{

    private Server _server;
    
    private boolean _noServerCleanup;

    private InetSocketAddress _addr;

    private ThreadPoolExecutor _executor;

    private Map<String, JettyHttpContext> _contexts = new HashMap<String, JettyHttpContext>();
    
    private Map<String, Connector> _connectors = new HashMap<String, Connector>();

    
    public JettyHttpServer(Server server, boolean noServerCleanUp)
    {
        this._server = server;
        this._noServerCleanup = noServerCleanUp;
    }

    @Override
    public void bind(InetSocketAddress addr, int backlog) throws IOException
    {
        this._addr = addr;
        
    	// check if there is already a connector listening
        Connector[] connectors = _server.getConnectors();
        if (connectors != null)
        {
	        for (int i = 0; i < connectors.length; i++)
	        {
				if (connectors[i].getPort() == addr.getPort())
				{
					if (Log.isDebugEnabled()) Log.debug("server already bound to port " + addr.getPort() + ", no need to rebind");
					return;
				}
			}
        }
        
        if (_executor != null && _server.getThreadPool() == null)
        {
        	if (Log.isDebugEnabled()) Log.debug("using given Executor for server thread pool");
        	_server.setThreadPool(new ThreadPoolExecutorAdapter(_executor));
        }

        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setAcceptors(1);
        connector.setPort(addr.getPort());
        connector.setHost(addr.getHostName());
        _server.addConnector(connector);
        
        _connectors.put(addr.getHostName() + addr.getPort(), connector);
    }

    @Override
    public InetSocketAddress getAddress()
    {
        return _addr;
    }

    @Override
    public void start()
    {
    	if (_noServerCleanup) return;
    	
        try
        {
            _server.start();
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void setExecutor(Executor executor)
    {
    	if (!(executor instanceof ThreadPoolExecutor))
    		throw new IllegalArgumentException("only ThreadPoolExecutor are allowed");
    	
        this._executor = (ThreadPoolExecutor) executor;
    }

    @Override
    public Executor getExecutor()
    {
        return _executor;
    }

    @Override
    public void stop(int delay)
    {
    	cleanUpContexts();
    	cleanUpConnectors();
    	
    	if (_noServerCleanup) return;

    	try
        {
            _server.stop();
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

	private void cleanUpContexts()
	{
		Iterator<Map.Entry<String, JettyHttpContext>> it = _contexts.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<String, JettyHttpContext> entry = (Map.Entry<String, JettyHttpContext>) it.next();
			JettyHttpContext context = entry.getValue();
			_server.removeHandler(context.getJettyContextHandler());
		}
		_contexts.clear();
	}

    private void cleanUpConnectors()
    {
		Iterator<Map.Entry<String, Connector>> it = _connectors.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<String, Connector> entry = (Map.Entry<String, Connector>) it.next();
			Connector connector = entry.getValue();
			try
			{
				connector.stop();
			} catch (Exception ex)
			{
				Log.warn(ex);
			}
			_server.removeConnector(connector);
		}
		_connectors.clear();
	}

	@Override
    public HttpContext createContext(String path, HttpHandler handler)
    {
    	checkIfContextIsFree(path);

        JettyHttpContext context = new JettyHttpContext(this, path, handler);
        J2SE6ContextHandler jettyContextHandler = context.getJettyContextHandler();
        HandlerCollection hc = null;
        
        Handler[] handlers = _server.getHandlers();
        for (int i = 0; i < handlers.length; i++)
        {
        	if (handlers[i] instanceof HandlerCollection)
        	{
        		hc = (HandlerCollection) handlers[i];
        		break;
        	}
		}
        if (hc == null)
        	throw new RuntimeException("could not find HandlerCollection, you must configure one");
        
        try
        {
        	jettyContextHandler.start();
		}
        catch (Exception ex)
        {
			throw new RuntimeException("could not start created context at path " + jettyContextHandler.getContextPath(), ex);
		}

        hc.addHandler(jettyContextHandler);
        _contexts.put(path, context);
        
        return context;
    }

    private void checkIfContextIsFree(String path)
    {
    	Handler handler = _server.getHandler();
		if (handler instanceof ContextHandler)
		{
			ContextHandler ctx = (ContextHandler) handler;
			if (ctx.getContextPath().equals(path))
	        	throw new RuntimeException("another context already bound to path " + path);
		}
    	
    	Handler[] handlers = _server.getHandlers();
    	if (handlers == null) return;
    	
    	for (int i = 0; i < handlers.length; i++)
    	{
			if (handlers[i] instanceof ContextHandler)
			{
				ContextHandler ctx = (ContextHandler) handlers[i];
				if (ctx.getContextPath().equals(path))
		        	throw new RuntimeException("another context already bound to path " + path);
			}
		}
	}

	@Override
    public HttpContext createContext(String path)
    {
        return createContext(path, null);
    }

    @Override
    public void removeContext(String path) throws IllegalArgumentException
    {
        JettyHttpContext context = _contexts.remove(path);
        if (context == null) return;
        _server.removeHandler(context.getJettyContextHandler());
    }

    @Override
    public void removeContext(HttpContext context)
    {
        removeContext(context.getPath());
    }

}

//========================================================================
//$$Id: JettyHttpExchange.java 549 2007-11-02 12:41:46Z lorban $$
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

/**
 * Jetty implementation of {@link com.sun.net.httpserver.HttpExchange}
 * @author lorban
 */
public class JettyHttpExchange extends HttpExchange
{

    private HttpContext _context;

    private HttpServletRequest _req;

    private HttpServletResponse _resp;

    private Headers _responseHeaders = new Headers();

    private int _responseCode = 0;

    private InputStream _is;

    private OutputStream _os;

	private HttpPrincipal _principal;
	

    public JettyHttpExchange(HttpContext context, HttpServletRequest req,
            HttpServletResponse resp)
    {
        this._context = context;
        this._req = req;
        this._resp = resp;
        try
        {
            this._is = req.getInputStream();
            this._os = resp.getOutputStream();
        }
        catch (IOException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Headers getRequestHeaders()
    {
        Headers headers = new Headers();
        Enumeration en = _req.getHeaderNames();
        while (en.hasMoreElements())
        {
            String name = (String) en.nextElement();
            Enumeration en2 = _req.getHeaders(name);
            while (en2.hasMoreElements())
            {
                String value = (String) en2.nextElement();
                headers.add(name, value);
            }
        }
        return headers;
    }

    @Override
    public Headers getResponseHeaders()
    {
        return _responseHeaders;
    }

    @Override
    public URI getRequestURI()
    {
        try
        {
        	String uriAsString = _req.getRequestURI();
        	if (_req.getQueryString() != null)
        		uriAsString += "?" + _req.getQueryString();
        	
            return new URI(uriAsString);
        }
        catch (URISyntaxException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String getRequestMethod()
    {
        return _req.getMethod();
    }

    @Override
    public HttpContext getHttpContext()
    {
        return _context;
    }

    @Override
    public void close()
    {
        try
        {
            _resp.getOutputStream().close();
        }
        catch (IOException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public InputStream getRequestBody()
    {
        return _is;
    }

    @Override
    public OutputStream getResponseBody()
    {
        return _os;
    }

    @Override
    public void sendResponseHeaders(int rCode, long responseLength)
            throws IOException
    {
        this._responseCode = rCode;

        Iterator it = _responseHeaders.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry entry = (Map.Entry) it.next();
            String name = (String) entry.getKey();
            List values = (List) entry.getValue();
            for (int i = 0; i < values.size(); i++)
            {
                String value = (String) values.get(i);
                _resp.setHeader(name, value);
            }
        }
        if (responseLength > 0)
            _resp.setHeader("content-length", "" + responseLength);
        _resp.setStatus(rCode);
    }

    @Override
    public InetSocketAddress getRemoteAddress()
    {
        return new InetSocketAddress(_req.getRemoteAddr(), _req.getRemotePort());
    }

    @Override
    public int getResponseCode()
    {
        return _responseCode;
    }

    @Override
    public InetSocketAddress getLocalAddress()
    {
        return new InetSocketAddress(_req.getLocalAddr(), _req.getLocalPort());
    }

    @Override
    public String getProtocol()
    {
        return _req.getProtocol();
    }

    @Override
    public Object getAttribute(String name)
    {
        return _req.getAttribute(name);
    }

    @Override
    public void setAttribute(String name, Object value)
    {
        _req.setAttribute(name, value);
    }

    @Override
    public void setStreams(InputStream i, OutputStream o)
    {
        _is = i;
        _os = o;
    }

    @Override
    public HttpPrincipal getPrincipal()
    {
    	return _principal;
    }
    
    public void setPrincipal(HttpPrincipal principal)
    {
    	this._principal = principal;
    }

}

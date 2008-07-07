package orc.orchard.soap;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletContext;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import com.sun.xml.ws.developer.JAXWSProperties;

/**
 * Mapping information to and from URLs is non-trivial, and
 * depends on how the servlets are deployed. I'm putting the
 * mapping in one class so it's relatively easy to change if
 * we change the way servlets are deployed.
 * 
 * <p>This class assumes the following URL scheme:
 * <tt>/CONTEXT/(json|soap)/(executor|jobs)/DEVKEY/JOBID</tt>
 * @author quark
 */
class URLHelper {
	static ServletContext getServletContext(WebServiceContext context) {
		MessageContext mc = context.getMessageContext();
		return (ServletContext)mc.get(MessageContext.SERVLET_CONTEXT);
	}
	static URL getRequestURL(WebServiceContext context) {
		try {
			return new URL((String)context.getMessageContext().get(
					JAXWSProperties.HTTP_REQUEST_URL));
		} catch (MalformedURLException e) {
			// Impossible by construction
			throw new AssertionError(e);
		}
	}
	static URL getJobServiceURL(WebServiceContext context, String jobID) {
		URL requestURL = getRequestURL(context);
		System.out.println("getJobServiceURL:" + requestURL);
		String[] path = requestURL.getPath().split("/");
		try {
			return new URL(requestURL,
					// context
					"/"+path[1]+
					// json or soap
					"/"+path[2]+
					// which service
					"/jobs"+
					// developer key
					"/"+getDeveloperKey(context)+
					// job ID
					"/"+jobID);
		} catch (MalformedURLException e) {
			// Impossible by construction
			throw new AssertionError(e);
		}
	}
	static String getDeveloperKey(WebServiceContext context) {
		URL requestURL = getRequestURL(context);
		System.out.println("getDeveloperKey:" + requestURL);
		String[] path = requestURL.getPath().split("/");
		if (path.length >= 5) {
			return path[4];
		} else {
			return "";
		}
	}
	static String getJobID(WebServiceContext context) {
		URL requestURL = getRequestURL(context);
		System.out.println("getJobID:" + requestURL);
		String[] path = requestURL.getPath().split("/");
		if (path.length >= 6) {
			return path[5];
		} else {
			return null;
		}
	}
}

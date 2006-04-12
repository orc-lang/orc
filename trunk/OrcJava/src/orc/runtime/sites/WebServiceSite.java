package orc.runtime.sites;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.async.Callback;
import org.apache.axis2.context.MessageContextConstants;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.soap.SOAPEnvelope;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.plugins.webservice.WebServicePlugin;
import orc.runtime.plugins.webservice.WebServiceSiteCallback;
import orc.runtime.sites.Site;
import orc.runtime.values.Tuple;

public abstract class WebServiceSite extends Site
{
	protected WebServicePlugin myParent;
	private String myMethodName;
	
	
	public WebServiceSite(WebServicePlugin parent,String methodName)
	{
		myParent = parent;
		myMethodName = methodName;
	}
	
	public abstract OMElement createRequest(Object[] paramVals);
	public abstract Object getResponse(SOAPEnvelope  response);
	public void setOptionsForCall(Options options)
	{
        //TODO: figure out what the following line does
        options.setProperty(MessageContextConstants.CHUNKED, Constants.VALUE_FALSE);
   
        //TODO: figure out what the following line does
        options.setTransportInProtocol(Constants.TRANSPORT_HTTP);
	}
	
	/**
	 * 
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.OrcEngine)
	 */
	final void callSite(Object[] args, Token returnToken, OrcEngine engine) 
	{
		String wsSiteName = webServiceSiteName();
		try
		{
	        OMElement requestElement = createRequest(args);
	        
	        Options options = new Options();
	        options.setTo(new EndpointReference(myParent.url().toString()));  
	        setOptionsForCall(options);	        
	        //The boolean flag informs the axis2 engine to use two separate transport connection
	        //to retrieve the response.
	        //options.setUseSeparateListener(true); 
	        
	        ServiceClient serviceClient = new ServiceClient();
	        serviceClient.setOptions(options);       
	        WebServiceSiteCallback cb = new WebServiceSiteCallback(this,returnToken,engine);
	        //Non-Blocking Invocation
	        serviceClient.sendReceiveNonblocking(requestElement, cb);
	        engine.addCall(1);
		} 
		catch (AxisFault axisFault) 
		{
			throw new Error("call to webservice site "+wsSiteName+" failed: "+axisFault.getMessage());
		} 
		catch (Exception ex) 
		{
			throw new Error("call to webservice site "+wsSiteName+" failed: "+ex.getMessage());
		}
	}
	
	public final String webServiceSiteName()
	{
		return myParent.varName()+"."+myMethodName;
	}
	
}

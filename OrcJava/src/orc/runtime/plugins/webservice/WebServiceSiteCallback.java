package orc.runtime.plugins.webservice;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.sites.WebServiceSite;

import org.apache.axis2.client.async.AsyncResult;
import org.apache.axis2.client.async.Callback;

public class WebServiceSiteCallback extends Callback 
{
	private Token myReturnToken;
	private OrcEngine myOrcEngine;
	private WebServiceSite myWSSite;
	
	public WebServiceSiteCallback(WebServiceSite wsSite,Token returnToken,OrcEngine engine)
	{
		myReturnToken = returnToken;
		myOrcEngine = engine;
		myWSSite = wsSite;
	}
	
    public void onComplete(AsyncResult result)
	{
    	Object value = myWSSite.getResponse(result.getResponseEnvelope());
		myReturnToken.setResult(value);
		myOrcEngine.activate(myReturnToken);
		myOrcEngine.removeCall();
    }

    public void onError(Exception e) 
	{
    	myOrcEngine.removeCall();
    	throw new Error("call to "+myWSSite.webServiceSiteName()+" returned the following error: "+e.getMessage());
    }
}

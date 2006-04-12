package orc.runtime.plugins.webservice.google;

import java.util.HashMap;
import java.util.Map;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;


import orc.runtime.plugins.webservice.WebServicePlugin;
import orc.runtime.sites.WebServiceSite;


import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMNamespace;
import org.apache.axis2.soap.SOAPBody;
import org.apache.axis2.soap.SOAPEnvelope;
import org.apache.axis2.soap.SOAPFactory;

public class GooglePlugin extends WebServicePlugin 
{
	//web service methods supported by this plugin 
	public static final String SPELLCHECK = "spellcheck";
	
	//properties that must be specifed in the properties file for this plugin
	public static final String GOOGLE_KEY = "google.key";
	public static final String GOOGLE_URL = "google.url";
	public static final String GOOGLE_SERVICENAME = "google.servicename";
	
	//other constatns vals
	public static final String PROTOCOL = "http";
	
	private Map<String,WebServiceSite> methodNameToSite = new HashMap<String,WebServiceSite>();
	private String myKey = null;
	private URL myURL = null;
	
	
	
	public GooglePlugin(String propertiesFilename,String name)
	{
		super(propertiesFilename,name);
		myKey = myProperties.getProperty(GOOGLE_KEY);
		if(myKey == null)
		{
			throw new Error("webservice "+name+" cannot be instantiated because no value specified for "+GOOGLE_KEY+" property.");
		}
		String endpointURL = myProperties.getProperty(GOOGLE_URL);
		if(endpointURL == null)
		{
			throw new Error("webservice "+name+" cannot be instantiated because no value specified for "+GOOGLE_URL+" property.");
		}
		String endpointServiceName = myProperties.getProperty(GOOGLE_SERVICENAME);
		if(endpointServiceName == null)
		{
			throw new Error("webservice "+name+" cannot be instantiated because no value specified for "+GOOGLE_SERVICENAME+" property.");
		}
		try
		{
			myURL = new URL(PROTOCOL,endpointURL,endpointServiceName);
		}
		catch(MalformedURLException e)
		{
			throw new Error("webservice "+name+" cannot be instantiated because invalid value specifed for a property used to create URL: "+e.getMessage());
		}
		methodNameToSite.put(SPELLCHECK,new SpellcheckSite(this,SPELLCHECK));
	}
	
	public URL url()
	{
		return myURL;
	}
	
	public String key()
	{
		return myKey;
	}
	
	@Override
	protected WebServiceSite getWebServiceSite(String methodName) 
	{
		if(methodNameToSite.containsKey(methodName))
		{
			return methodNameToSite.get(methodName);
		}
		else
		{
			throw new Error("webservice "+varName()+"does not support method "+methodName);
		}
	}
}

class SpellcheckSite extends WebServiceSite 
{
	
	public SpellcheckSite(GooglePlugin parent,String methodName)
	{
		super(parent,methodName);
	}
	
	@Override
	public OMElement createRequest(Object[] paramVals) 
	{
		String wsSiteName = webServiceSiteName();
		String word;
		if(paramVals == null)
		{
			throw new Error("call to webservice site "+wsSiteName+" failed: 1 arg of type String expected received a null arg list");
		}
		else if(paramVals.length != 1)
		{
			throw new Error("call to webservice site "+wsSiteName+" failed: 1 arg of type String expected received an arg list of length "+paramVals.length);
		}
		try
		{
			word = (String)paramVals[0];
		}
		catch(ClassCastException e)
		{
			throw new Error("call to webservice site "+wsSiteName+" failed: 1 arg of type String expected received a non string arg");
		}
        SOAPFactory omfactory = OMAbstractFactory.getSOAP11Factory();
        OMNamespace opN = omfactory.createOMNamespace("urn:GoogleSearch",
                "ns1");
        OMNamespace emptyNs = omfactory.createOMNamespace("", null);

        OMElement method = omfactory.createOMElement("doSpellingSuggestion",
                opN);
        method.declareNamespace("http://www.w3.org/1999/XMLSchema-instance",
                "xsi");
        method.declareNamespace("http://www.w3.org/1999/XMLSchema", "xsd");

        //reqEnv.getBody().addChild(method);
        method.addAttribute("soapenv:encodingStyle",
                "http://schemas.xmlsoap.org/soap/encoding/",
                null);
        OMElement value1 = omfactory.createOMElement("key", emptyNs);
        OMElement value2 = omfactory.createOMElement("phrase", emptyNs);
        value1.addAttribute("xsi:type", "xsd:string", null);
        value2.addAttribute("xsi:type", "xsd:string", null);
        value1.addChild(
                omfactory.createText(value1, ((GooglePlugin)myParent).key()));
        value2.addChild(omfactory.createText(value2, word));

        method.addChild(value1);
        method.addChild(value2);
        return method;
	}

	@Override
	public Object getResponse(SOAPEnvelope response) 
	{
		String wsSiteName = webServiceSiteName();
		
        QName qName1 = new QName("urn:GoogleSearch",
        "doSpellingSuggestionResponse");

		SOAPBody body = response.getBody();
		if (body.hasFault()) 
		{
			throw new Error("call to webservice site "+wsSiteName+" returned a faulty response. fault description: "+body.getFault().getException().getMessage());
		} 
		else 
		{
		    OMElement root = body.getFirstChildWithName(qName1);
		    OMElement val;
		    if (root != null) {
		        // val = root.getFirstChildWithName(qName2);
		        val = root.getFirstElement();
		    } 
		    else 
		    {
		    	throw new Error("call to webservice site "+wsSiteName+"did not return a correct response!");
		    }
		
		    String sugession = val.getText();
		    if ((sugession == null) || (sugession.trim().equals(""))) 
		    {
		        return "no suggestions";
		    } 
		    else 
		    {
		        return sugession;
		    }
		}
	}
}


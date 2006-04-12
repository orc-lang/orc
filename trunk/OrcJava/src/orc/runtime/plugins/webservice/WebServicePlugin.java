package orc.runtime.plugins.webservice;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import orc.runtime.DotAccessible;
import orc.runtime.sites.WebServiceSite;
import orc.runtime.values.BaseValue;

public abstract class WebServicePlugin implements DotAccessible
{
	protected Properties myProperties = null;
	private String myVarName = null;
	
	public WebServicePlugin(String propertiesFilename,String name)
	{
        try 
        {
        	myVarName = name;
            myProperties = new Properties();
            InputStream stream = Object.class.getResourceAsStream(propertiesFilename);
            myProperties.load(stream);
        } 
        catch (IOException e)
        {
            throw new Error("web service "+myVarName+
            				" could not be instantiated its properties could not be load from file"+
            				propertiesFilename+": "+e.getMessage());
            
        }
	}
	
	public abstract URL url();
	
	public String varName()
	{
		return myVarName;
	}
	
	protected abstract WebServiceSite getWebServiceSite(String methodName);
	
	public BaseValue dotAccessibleVal(String methodName)
	{
		return getWebServiceSite(methodName);
	}
}
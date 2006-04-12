package orc.ast;

import java.lang.reflect.Constructor;

import orc.runtime.nodes.Assign;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Param;

public class WebService extends OrcProcess 
{
	private String myName;
	private String myPluginClassName;
	private String myPropertiesFilename;

	public WebService(String name,String pluginClassName,String propertiesFilename) 
	{
		myName = name;
		myPluginClassName = pluginClassName;
		myPropertiesFilename = propertiesFilename;
	}

	public String toString() 
	{
		return "webservice "+myName+" "+myPluginClassName+" "+myPropertiesFilename+"\n";
	}
	
	/**
	 * Creates a literal node with an instance of webservice plugin as argument
	 * followed by an assignment of this literal to the name to bind the 
	 * webservice plugin to that name 
	 * @see orc.ast.OrcProcess#compile(orc.runtime.nodes.Node)
	 */
	public Node compile(Node output) 
	{
		Node retVal;
		try
		{
			Class myPluginClass = Class.forName(myPluginClassName);
			Class[] argTypes = new Class[]{String.class,String.class}; 
			Constructor myPluginConstructor = myPluginClass.getConstructor(argTypes);
			Object[] args = new Object[]{myPropertiesFilename,myName};
			Object value = myPluginConstructor.newInstance(args);
			Assign assign = new Assign(myName,output);
			retVal = new orc.runtime.nodes.Literal(value, assign);
		}
		catch(Exception e)
		{
			throw new Error("Webservice could not be instantiated: "+e.getMessage());
		}
		return retVal;
	}

}

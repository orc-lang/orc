package orc.ast.sites;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;

import orc.error.compiletime.typing.MissingTypeException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.type.Type;

public class OrcSite extends Site {

	protected URI location;
	
	public OrcSite(URI location) {
		this.location = location;
	}
	
	public URI getLocation() {
		return location;
	}

	public String getProtocol() {
		return ORC;
	}

	public Class classify() {
		String classname = location.getSchemeSpecificPart();
		Class<?> cls;
		
		try {
			cls = Class.forName(classname);
		}
		catch (ClassNotFoundException e) {
			throw new Error("Failed to load class " + classname + " as a site. Class not found.");
		}
		
		if (orc.runtime.sites.Site.class.isAssignableFrom(cls)) {
			return cls;
		}
		else { 
			throw new Error("Class " + cls + " cannot be used as a site because it is not a subtype of orc.runtime.sites.Site."); 
		}
	}
	
	public orc.runtime.sites.Site instantiate() {
		
		Class cls = classify();
		try
		{
			return (orc.runtime.sites.Site)(cls.newInstance());
		} catch (InstantiationException e) {
			throw new Error("Failed to load class " + cls + " as a site. Instantiation error.", e);
		} catch (IllegalAccessException e) {
			throw new Error("Failed to load class " + cls + " as a site. Constructor is not accessible.");
		}
		
	}

	public Type type() throws TypeException {
		
		Class cls = classify();
		try {
			return (Type)cls.getMethod("type", new Class[]{}).invoke(null, new Object[]{});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new MissingTypeException("Couldn't access type of site " + cls + " due to error: " + e);
		}
	}
	

}

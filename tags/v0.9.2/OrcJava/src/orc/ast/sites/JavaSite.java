package orc.ast.sites;

import java.net.URI;
import java.net.URISyntaxException;

import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.java.ClassProxy;

public class JavaSite extends Site {

	protected URI location;
	
	public JavaSite(URI location) {
		this.location = location;
	}
	
	public URI getLocation() {
		return location;
	}

	public String getProtocol() {
		return JAVA;
	}

	public orc.runtime.sites.Site instantiate() {
		
		String classname = location.getSchemeSpecificPart();
		try
		{
			Class<?> cls = Class.forName(classname);
			
			return new ClassProxy(cls);
		}
		catch (Exception e) { throw new Error("Failed to load class " + classname + " as a proxy.", e); }
	}

}

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

	public Class classify() {
		String classname = location.getSchemeSpecificPart();
		try
		{
			Class<?> cls = Class.forName(classname);
			return cls;
		}
		// TODO: Make this more informative than Error
		catch (Exception e) { throw new Error("Failed to load class " + classname, e); }
	}
	
	public orc.runtime.sites.Site instantiate() {
		return ClassProxy.forClass(classify());
	}
	
}

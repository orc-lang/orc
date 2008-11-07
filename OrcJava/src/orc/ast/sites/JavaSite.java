package orc.ast.sites;

import java.net.URI;
import java.net.URISyntaxException;

import orc.error.runtime.SiteResolutionException;
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

	public Class classify() throws SiteResolutionException {
		String classname = location.getSchemeSpecificPart();
		try
		{
			Class<?> cls = Class.forName(classname);
			if (orc.runtime.sites.Site.class.isAssignableFrom(cls)) {
				throw new SiteResolutionException("Tried to load a subclass of orc.runtime.sites.Site as a Java class -- that's not allowed!");
			}
			return cls;
		} catch (ClassNotFoundException e) {
			throw new SiteResolutionException("Failed to load class " + classname, e);
		}
	}
	
	public orc.runtime.sites.Site instantiate() throws SiteResolutionException {
		return ClassProxy.forClass(classify());
	}
	
	public String toString() {
		return "java:" + location;
	}
}

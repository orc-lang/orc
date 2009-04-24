package orc.ast.sites;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;

import orc.Config;
import orc.error.compiletime.SiteResolutionException;
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

	private Class classify(Config config) throws SiteResolutionException {
		String classname = location.getSchemeSpecificPart();
		Class<?> cls;
		
		try {
			cls = config.loadClass(classname);
		}
		catch (ClassNotFoundException e) {
			throw new SiteResolutionException("Failed to load class " + classname + " as a site. Class not found.");
		}
		
		if (orc.runtime.sites.Site.class.isAssignableFrom(cls)) {
			return cls;
		}
		else { 
			throw new SiteResolutionException("Class " + cls + " cannot be used as a site because it is not a subtype of orc.runtime.sites.Site."); 
		}
	}
	
	public orc.runtime.sites.Site instantiate(Config config) throws SiteResolutionException {
		
		Class cls = classify(config);
		try
		{
			return (orc.runtime.sites.Site)(cls.newInstance());
		} catch (InstantiationException e) {
			throw new SiteResolutionException("Failed to load class " + cls + " as a site. Instantiation error.", e);
		} catch (IllegalAccessException e) {
			throw new SiteResolutionException("Failed to load class " + cls + " as a site. Constructor is not accessible.");
		}
		
	}
}

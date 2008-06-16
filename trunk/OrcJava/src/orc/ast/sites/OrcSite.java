package orc.ast.sites;

import java.net.URI;
import java.net.URISyntaxException;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.Token;

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

	public orc.runtime.sites.Site instantiate() {
		
		String classname = location.getSchemeSpecificPart();
		try
		{
			Class<?> cls = ClassLoader.getSystemClassLoader().loadClass(classname);
			
			if (orc.runtime.sites.Site.class.isAssignableFrom(cls)) 
			{
				return (orc.runtime.sites.Site)cls.newInstance();
			}
			else
			{ 
				throw new Error("Class " + cls + " cannot be used as a site. It is not a subtype of Site."); 
			}
		}
		catch (Exception e) { throw new Error("Failed to load class " + classname + " as a site."); }
	}

}

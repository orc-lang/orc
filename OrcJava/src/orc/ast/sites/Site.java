package orc.ast.sites;

import java.net.URI;
import java.net.URISyntaxException;

import orc.Config;
import orc.error.OrcError;
import orc.error.compiletime.SiteResolutionException;
import orc.error.compiletime.typing.MissingTypeException;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;

/**
 * 
 * A portable representation of sites. When creating the execution graph, 
 * these are converted to in-memory objects.
 * 
 * @author dkitchin
 *
 */
public abstract class Site {

	/* Protocols */
	public static final String ORC = "orc";
	public static final String JAVA = "java";
	
	/* Primitive sites */
	public static Site LET = buildCoreSite("Let");
	public static Site ERROR = buildCoreSite("Error");

	public static Site IF = buildCoreSite("If");
	public static Site NOT = buildCoreSite("Not");
	
	public static Site SOME = buildCoreSite("Some");
	public static Site NONE = buildCoreSite("None");

	public static Site ISSOME = buildCoreSite("IsSome");
	public static Site ISNONE = buildCoreSite("IsNone");

	public static Site CONS = buildCoreSite("Cons");
	public static Site NIL = buildCoreSite("Nil");
	
	public static Site TRYCONS = buildCoreSite("TryCons");
	public static Site TRYNIL = buildCoreSite("TryNil");
	
	public static Site EQUAL = buildCoreSite("Equal");
	
	public static Site DATATYPE = buildCoreSite("Datatype");
	
	public static Site build(String protocol, String location) {
		try {
			return build(protocol, new URI(location));
		} catch (URISyntaxException e) {
			throw new OrcError(location + " is not a valid URI");
		}
	}
	
	public static Site build(String protocol, URI location) {
		
		if (protocol.equals(ORC)) {
			return new OrcSite(location);
		}
		else if (protocol.equals(JAVA)) {
			return new JavaSite(location);
		}
		else {
			throw new OrcError("'" + protocol + "' is not a supported protocol.");
		}
	}

	/* Specialization of build for primitive sites */
	public static Site buildCoreSite(String primitive) {
		String location = "orc.runtime.sites.core." + primitive;
		return build(ORC, location);
	}
	
	public abstract URI getLocation();
	public abstract String getProtocol();	
	public abstract orc.runtime.sites.Site instantiate(Config config) throws SiteResolutionException;

	/**
	 * Equality on sites.
	 * 
	 * Two sites are equal if their protocols are equal
	 * and their locations are equal.
	 * 
	 * @param that  The site to which to compare.
	 */
	public boolean equals(Site that) {
		return (  this.getLocation().equals(that.getLocation())
			   && this.getProtocol().equals(that.getProtocol()) );
	}

	public String toString() {
		return "#site(" + getProtocol() + ", " + getLocation() + ")";
	}
}

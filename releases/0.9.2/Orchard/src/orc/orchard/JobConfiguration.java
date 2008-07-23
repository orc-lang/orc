package orc.orchard;

import java.io.Serializable;

/**
 * JAXB does bad things if you extend another class
 * which is not specifically designed to be JAXB-marshalled.
 * So we can't inherit any implementation of this class, which
 * is OK since it's trivial anyways.
 * @author quark
 */
public class JobConfiguration implements Serializable {
	public boolean debuggable = false;
	
	public JobConfiguration() {}
}

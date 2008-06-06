package orc.orchard.interfaces;

import java.io.Serializable;

/**
 * Configuration for a job execution.
 * @author quark
 */
public interface JobConfiguration extends Serializable {
	public boolean getListenable();
	public boolean getDebuggable();
	public String getProtocol();
}
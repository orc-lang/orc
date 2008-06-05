package orc.orchard.interfaces;

/**
 * Configuration for a job execution.
 * @author quark
 */
public interface JobConfiguration {
	public boolean getListenable();
	public boolean getDebuggable();
	public String getProtocol();
}
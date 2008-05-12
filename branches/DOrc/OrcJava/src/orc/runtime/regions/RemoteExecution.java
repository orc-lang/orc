package orc.runtime.regions;

import java.rmi.RemoteException;

/**
 * This represents a slave execution in a distributed computation.
 * Unlike the master execution, it does not terminate the engine
 * when it completes, but instead notifies the master execution. 
 * @author quark
 */
public class RemoteExecution extends Region {
	private static final long serialVersionUID = 1L;
	RemoteRegion parent;
	public RemoteExecution(RemoteRegion parent) {
		super();
		this.parent = parent;
		// NOTE: do not grow the parent
		// because this region replaces
		// an existing token
	}
	
	protected void onClose() {
		try {
			parent.shrink();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException(e);
		}
	}
}

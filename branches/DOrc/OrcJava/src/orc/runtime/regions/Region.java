package orc.runtime.regions;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;


public abstract class Region implements RemoteRegion {
	int inhabitants = 0;
	protected boolean open = true;
	
	public void grow() {
		inhabitants++;
	}
		
	public void shrink() {
		inhabitants--;
		if (inhabitants <= 0) { close(); }
	}

	public boolean isOpen() { return open; }
	
	public void close() {
		if (!open) return;
		open = false;
		onClose();
	}
	
	/**
	 * Override this to perform cleanup when the region closes.
	 */
	protected abstract void onClose();
}

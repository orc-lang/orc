/**
 * 
 */
package orc.lib.state;

import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.Queue;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.RemoteToken;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 * Write-once mutable cell. 
 * Read operations block if the cell is empty.
 * Write operatons block if the cell is full.
 *
 */
public class Cell extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Value evaluate(Args args) {
		return new orc.runtime.values.Site(new CellInstance());
	}
	
	
	protected static class CellInstance extends DotSite {

		private Queue<RemoteToken> readQueue;
		Value contents;

		CellInstance() {
			this.contents = null;
			
			/* Note that the readQueue also signals whether the cell contents have been assigned.
			 * If it is non-null (as it is initially), the cell is empty.
			 * If it is null, the cell has been written.
			 * 
			 * This allows the cell to contain a null value if needed, and it also
			 * frees the memory associated with the read queue once the cell has been assigned.
			 */
			this.readQueue = new LinkedList<RemoteToken>();
		}
		
		@Override
		protected void addMethods() {
			addMethod("read", new readMethod());	
			addMethod("write", new writeMethod());
		}
		
		private class readMethod extends Site {
			@Override
			public void callSite(Args args, RemoteToken reader) {

				/* If the read queue is not null, the cell has not been set.
				 * Add this caller to the read queue.
				 */ 
				if (readQueue != null) {
					readQueue.add(reader);
				}
				/* Otherwise, return the contents of the cell */
				else {
					try {
						reader.resume(contents);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						throw new RuntimeException(e);
					}
				}
			}
		}
		
		private class writeMethod extends Site {
			@Override
			public void callSite(Args args, RemoteToken writer) throws OrcRuntimeTypeException {

				Value val = args.valArg(0);
				
				/* If the read queue is not null, the cell has not yet been set. */
				if (readQueue != null) {
					/* Set the contents of the cell */
					contents = val;
					
					/* Wake up all queued readers and report the written value to them. */
					for (RemoteToken reader : readQueue) {
						try {
							reader.resume(val);
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							throw new RuntimeException(e);
						}
					}
					
					/* Null out the read queue. 
					 * This indicates that the cell has been written.
					 * It also allowed the associated memory to be reclaimed.
					 */
					readQueue = null;
					
					/* A successful write publishes a signal. */
					try {
						writer.resume();
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						throw new RuntimeException(e);
					}
				} else {
					/* A failed write kills the writer. */
					try {
						writer.die();
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						throw new RuntimeException(e);
					}
				}
			}
		}
	}
}

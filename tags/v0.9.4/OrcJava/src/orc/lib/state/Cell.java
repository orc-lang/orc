/**
 * 
 */
package orc.lib.state;

import java.util.LinkedList;
import java.util.Queue;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;

/**
 * @author dkitchin
 *
 * Write-once cell. 
 * Read operations block while the cell is empty.
 * Write operatons fail once the cell is full.
 *
 */
public class Cell extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Object evaluate(Args args) {
		return new CellInstance();
	}
	
	
	protected class CellInstance extends DotSite {

		private Queue<Token> readQueue;
		Object contents;

		CellInstance() {
			this.contents = null;
			
			/* Note that the readQueue also signals whether the cell contents have been assigned.
			 * If it is non-null (as it is initially), the cell is empty.
			 * If it is null, the cell has been written.
			 * 
			 * This allows the cell to contain a null value if needed, and it also
			 * frees the memory associated with the read queue once the cell has been assigned.
			 */
			this.readQueue = new LinkedList<Token>();
		}
		
		@Override
		protected void addMethods() {
			addMethod("read", new readMethod());	
			addMethod("write", new writeMethod());
		}
		
		private class readMethod extends Site {
			@Override
			public void callSite(Args args, Token reader) {

				/* If the read queue is not null, the cell has not been set.
				 * Add this caller to the read queue.
				 */ 
				if (readQueue != null) {
					readQueue.add(reader);
				}
				/* Otherwise, return the contents of the cell */
				else {
					reader.resume(contents);
				}
			}
		}
		
		private class writeMethod extends Site {
			@Override
			public void callSite(Args args, Token writer) throws TokenException {

				Object val = args.getArg(0);
				
				/* If the read queue is not null, the cell has not yet been set. */
				if (readQueue != null) {
					/* Set the contents of the cell */
					contents = val;
					
					/* Wake up all queued readers and report the written value to them. */
					for (Token reader : readQueue) {
						reader.resume(val);
					}
					
					/* Null out the read queue. 
					 * This indicates that the cell has been written.
					 * It also allowed the associated memory to be reclaimed.
					 */
					readQueue = null;
					
					/* A successful write publishes a signal. */
					writer.resume();
				}
				else {
					/* A failed write kills the writer. */
					writer.die();
				}
					
					
			}
		}

	}
	
}

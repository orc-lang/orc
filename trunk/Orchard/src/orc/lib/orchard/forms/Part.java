package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;


public interface Part<V> {
	/** Render this form part to the given response using its printwriter. 
	 * @throws IOException */
	public void render(PrintWriter out, Set<String> flags) throws IOException;
	/** Return the value of the part. */
	public V getValue();
	/** Read a value from the request. Append any error messages to the given list. */
	public void readRequest(FormData request, List<String> errors);
	/** A unique name for this part. */
	public String getKey();
	/** True if this part needs to use form/multipart encoding */
	public boolean needsMultipartEncoding();
}

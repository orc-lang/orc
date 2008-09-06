package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

public interface Part<V> {
	/** Render this form part to the given response using its printwriter. 
	 * @throws IOException */
	public void render(PrintWriter out) throws IOException;
	/** Return the value of the part. */
	public V getValue();
	/** Read a value from the request. Append any error messages to the given list. */
	public void readRequest(HttpServletRequest request, List<String> errors);
	/** A unique name for this part. */
	public String getKey();
}

package orc.error.loadtime;

import orc.error.OrcException;

/**
 * 
 * Exceptions generated while creating an execution
 * graph from a portable representation, in preparation
 * for execution.
 * 
 * @author dkitchin
 *
 */
public class LoadingException extends OrcException {

	public LoadingException(String message) {
		super(message);
	}

}

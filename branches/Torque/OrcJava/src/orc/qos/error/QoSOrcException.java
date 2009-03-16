package orc.qos.error;

import orc.error.OrcException;

/**
 * Base class for all exceptions that occur in the 
 * compilation or execution of the QoS analysis module.
 * 
 * @author srosario
 *
 */
public class QoSOrcException extends OrcException {

	public QoSOrcException(String message) {
		super(message);
	}

}

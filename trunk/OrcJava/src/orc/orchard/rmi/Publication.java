package orc.orchard.rmi;

import java.io.Serializable;
import java.util.Date;

public class Publication implements orc.orchard.interfaces.Publication, Serializable {
	private int sequence;
	private Date timestamp;
	private Object value;
	public Publication(int sequence, Date timestamp, Object value) {
		this.sequence = sequence;
		this.timestamp = timestamp;
		this.value = value;
	}
	public int getSequence() {
		return sequence;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public Object getValue() {
		return value;
	}
	public String toString() {
		return super.toString() + "(" + sequence + ", " + timestamp + ", " + value + ")";
	}
}

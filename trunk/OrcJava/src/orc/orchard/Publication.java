package orc.orchard;

import java.util.Date;

/**
 * JAXB does bad things if you extend another class
 * which is not specifically designed to be JAXB-marshalled.
 * So we can't inherit any implementation of this class, which
 * is OK since it's trivial anyways.
 * @author quark
 */
public class Publication implements orc.orchard.interfaces.Publication {
	private int sequence;
	private Date timestamp;
	private Object value;
	
	public Publication() {}
	
	public Publication(int sequence, Date timestamp, Object value) {
		this();
		this.setSequence(sequence);
		this.setTimestamp(timestamp);
		this.setValue(value);
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
	
	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public String toString() {
		return super.toString() + "(" + sequence + ", " + timestamp + ", " + value + ")";
	}
}
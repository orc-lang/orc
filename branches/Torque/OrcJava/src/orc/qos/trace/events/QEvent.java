package orc.qos.trace.events;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import orc.error.Locatable;
import orc.error.SourceLocation;

/**
 * Base class for QoS-trace events.
 * @author quark,sidney
 */
public abstract class QEvent implements Serializable, Locatable {
	/**
	 * List of predecessors of the event.
	 */
	List<QEvent> preds;
	/** Start date of the event's execution */
	long startDate=0;
	/** Ending date of the event's execution */
	long endDate=0;
	
	protected SourceLocation location;
	
	public List<QEvent> getPreds() {
		return preds;
	}
	
	public void setPredecessors(List<QEvent> eList) {
		preds = eList;
	}
	
	public SourceLocation getSourceLocation() {
		return location;
	}

	public void setSourceLocation(SourceLocation location) {
		this.location = location;
	}
	
	public abstract <E> E accept(Visitor<E> v);
}

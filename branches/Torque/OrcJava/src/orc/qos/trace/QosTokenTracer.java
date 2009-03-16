package orc.qos.trace;

import java.util.LinkedList;
import java.util.List;

import orc.error.SourceLocation;
import orc.error.runtime.TokenException;
import orc.qos.trace.events.QEvent;
import orc.qos.trace.events.QForkEvent;
import orc.qos.trace.events.QHaltEvent;
import orc.qos.trace.events.QPublishEvent;
import orc.qos.trace.events.QPullEvent;
import orc.qos.trace.events.QReceiveEvent;
import orc.qos.trace.events.QSendEvent;
import orc.qos.trace.events.QStoreEvent;
import orc.runtime.values.Closure;
import orc.trace.TokenTracer;

/** 
 * The Token tracer for the QoS analysis. This class
 * builds the partial order of the execution events.
 * 
 * @author srosario
 *
 */
public class QosTokenTracer extends TokenTracer {
	/** The current source location (used for all events). */
	private SourceLocation location;
	
	/** The latest event in this thread */
	private QEvent currentEvent;
	
	/**
	 * Reference to the tracer that created this thread.
	 * This is used to track the creation of new threads
	 * from this thread.
	 */
	private QosTracer tracer;
	
	/**
	 * The set of events that cause the next event.
	 * This set is reset each time a new event is created.
	 */
	private List<QEvent> nextCauses;
	
	public QosTokenTracer(QEvent currentEvent,QosTracer tracer,SourceLocation location) {
		this.currentEvent=currentEvent;
		this.location=location;
		this.tracer=tracer;
		this.tracer.addTokenTracer(this);
		this.nextCauses=new LinkedList<QEvent>();
	}

	public QEvent getCurrentEvent() {
		return currentEvent;
	}
	
	public void after(BeforeTrace before) {	}

	public void block(PullTrace pull) {	}

	public void choke(StoreTrace store) { }
	
	public void die() {	}
	
	public void error(TokenException error) { }
	
	public TokenTracer fork() {
		QForkEvent f = new QForkEvent();
		updateCauses(f);
		
		QosTokenTracer t = new QosTokenTracer(f,tracer,location);
		
		return t; 
	}
	
	public void finishStore(StoreTrace event) {	}
	
	public void print(String value, boolean newline) { }
	
	public void publish(Object value) {
		QPublishEvent p = new QPublishEvent(value);
		updateCauses(p);
	}
	
	public PullTrace pull() {
		QPullEvent p = new QPullEvent();
		//updateCauses(p);
		
		return p;
	}
	
	public void receive(Object value) {	
		QEvent r = new QReceiveEvent(value);
		updateCauses(r);
	}
	
	public void send(Object site, Object[] arguments) {
		QSendEvent s = new QSendEvent(site);
		updateCauses(s);
	}
	
	public StoreTrace store(PullTrace event, Object value) {
		QStoreEvent s = new QStoreEvent(value);
		updateCauses(s);
		
		return s;		
	}
	
	public void unblock(StoreTrace store) {}
	
	public SourceLocation getSourceLocation() {
		return location;
	}
	
	public void setSourceLocation(SourceLocation location) {
		this.location=location;
	}

	public void useStored(StoreTrace storeTrace) {
		QStoreEvent s = (QStoreEvent) storeTrace; // This cast can't fail as QoSTokenTracer is used throughout tracing.
		
		if(!nextCauses.contains(storeTrace))
			nextCauses.add(s);
	}
	
	@Override
	public void enter(Closure closure) {}

	@Override
	public void leave(int depth) {}
	
	@Override
	public void enterOther(List<HaltTrace> haltCauses) {
		for(HaltTrace h : haltCauses)
		  nextCauses.add((QHaltEvent) h);
	}

	@Override
	public HaltTrace halt(List<HaltTrace> hList) {
		QHaltEvent halt = new QHaltEvent();

		if (hList!=null) {
		  for(HaltTrace h : hList)
			nextCauses.add((QHaltEvent) h);
		}
			
		updateCauses(halt);
		
		return halt;
	}
	
	public void updateCauses(QEvent newEvent) {
		nextCauses.add(currentEvent);
		newEvent.setPredecessors(nextCauses);
		currentEvent=newEvent;
		nextCauses=new LinkedList<QEvent>();
	}
}
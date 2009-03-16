package orc.qos.trace;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import orc.error.SourceLocation;
import orc.qos.trace.events.QEvent;
import orc.qos.trace.events.QForkEvent;
import orc.qos.trace.events.QHaltEvent;
import orc.qos.trace.events.QPublishEvent;
import orc.qos.trace.events.QPullEvent;
import orc.qos.trace.events.QReceiveEvent;
import orc.qos.trace.events.QSendEvent;
import orc.qos.trace.events.QStartEvent;
import orc.qos.trace.events.QStoreEvent;
import orc.qos.trace.events.Visitor;
import orc.trace.TokenTracer;
import orc.trace.Tracer;
import att.grappa.Attribute;
import att.grappa.Edge;
import att.grappa.Graph;
import att.grappa.Grappa;
import att.grappa.Node;

/**
 * Default tracer used in the QoS analysis. This tracer
 * uses {@link QosTokenTracer} which builds the partial
 * order of the execution events.
 * 
 * @author srosario
 *
 */
public class QosTracer extends Tracer {
	/** 
	 * We track the maximal events during any point
	 * of an orc execution by tracking each new thread
	 * created by the engine. A new thread is created by
	 * a 'fork' event which creates a new {@link QosTokenTracer} for it. 
	 */ 
	List<QosTokenTracer> threads;
	
	public QosTracer() {
		threads = new LinkedList<QosTokenTracer>();
	}
	
	public void finish() {
		String dotFile="qos-examples/output/trace.dot";
		printDotFile(dotFile);
	}

	public TokenTracer start() {
		QEvent startEvent = new QStartEvent();
	
		return new QosTokenTracer(startEvent,this,SourceLocation.UNKNOWN);
	}
	
	public void addTokenTracer(QosTokenTracer t) {
		threads.add(t);
	}
	
	/**
	 * Printing the dot file happens in a depth-first manner
	 * but starting from the maximal events of the execution.
	 * The plotted nodes are stored in a map to avoid them from
	 * being redrawn.
	 * 
	 * @param dotFileName
	 */
	void printDotFile(String dotFileName) {
		 HashMap<QEvent,Node> drawnEvents = new HashMap<QEvent,Node>();

		 Graph graph = new Graph(dotFileName);

	     for(QosTokenTracer tracer : threads) {
	    	 QEvent currentEvent = tracer.getCurrentEvent();
	    	 assert(currentEvent!=null);
	    	 
	    	 Node node = drawnEvents.get(currentEvent);
	    	 if(node==null) {
	    		 node = new Node(graph);
			     drawnEvents.put(currentEvent,node);
			     
			     visitor.setNode(node);
		    	 node = currentEvent.accept(visitor);
		    	 
			     drawRecursive(node,currentEvent,drawnEvents,graph);
	    	 }
	     }
	     
	     PrintWriter out;
	     try {
	    	 out = new PrintWriter(new FileWriter(dotFileName));
	    	 graph.printGraph(out);
	    	 out.close();
	     } catch (IOException e) {
	    	 e.printStackTrace();
	     }	
	}
	
	void drawRecursive(Node child,QEvent evt,Map<QEvent,Node> drawnEvents,Graph graph) {
		
	     List<QEvent> preds = evt.getPreds();
	 
	     if(preds != null)
		     for (QEvent e : preds) {
		    	 Node node = drawnEvents.get(e);
		    	 if(node!=null) {
		    		 Edge edge = new Edge(graph,node,	child);
		    	 } else {
		    		 node = new Node(graph,drawnEvents.size()+"");
		    		 drawnEvents.put(e,node);
				
			    	 visitor.setNode(node);
			    	 node = e.accept(visitor);
			    	 
			    	 Edge edge = new Edge(graph,node,child);
			    	 
			    	 // Recurse on the predecessor node.
			    	 drawRecursive(node,e,drawnEvents,graph);
		    	 }
		     }
	}
	
	BuildNode visitor = new BuildNode();
	
	class BuildNode implements Visitor<Node> { 
		Node node;
		
		public Node visit(QForkEvent e) {
			setCommon(e);
			node.setAttribute("fillcolor", "white");
			node.setAttribute("fontcolor", "blue");
			
			return node;
		}

		public Node visit(QHaltEvent e) {
			setCommon(e);
			node.setAttribute("fillcolor", "red");
			node.setAttribute("fontcolor", "black");
			
			return node;
		}

		public Node visit(QPublishEvent e) {
			setCommon(e);
			node.setAttribute("fillcolor", "green");
			node.setAttribute("fontcolor", "black");
			
			return node;
		}

		public Node visit(QPullEvent e) {
			setCommon(e);
			node.setAttribute("fillcolor", "white");
			node.setAttribute("fontcolor", "blue");
			
			return node;
		}

		public Node visit(QReceiveEvent e) {
			setCommon(e);
			node.setAttribute("fillcolor", "white");
			node.setAttribute("fontcolor", "blue");
			
			return node;
		}

		public Node visit(QSendEvent e) {
			setCommon(e);
			node.setAttribute("fillcolor", "yellow");
			node.setAttribute("fontcolor", "blue");
			
			return node;
		}

		public Node visit(QStartEvent e) {
			setCommon(e);
			node.setAttribute("fillcolor", "blue");
			node.setAttribute("fontcolor", "white");
			
			return node;
		}

		public Node visit(QStoreEvent e) {
			setCommon(e);
			node.setAttribute("fillcolor", "white");
			node.setAttribute("fontcolor", "blue");
			
			return node;
		}
		
		public void setNode(Node n) {
			node = n;
		}
		
		public void setCommon(QEvent e) {
			node.setAttribute("label", "<f0> " + e);
	    	node.setAttribute(new Attribute(Grappa.NODE, "shape", "record"));
	    	node.setAttribute(new Attribute(Grappa.NODE, "style", "filled"));
		}
	}
} 

package orc.runtime.nodes;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;

import javax.swing.JFrame;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.EdgeShape;
import edu.uci.ics.jung.graph.decorators.EdgeStringer;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.decorators.ToStringLabeller;
import edu.uci.ics.jung.graph.decorators.VertexStringer;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.DirectedSparseVertex;
import edu.uci.ics.jung.visualization.FRLayout;
import edu.uci.ics.jung.visualization.ISOMLayout;
import edu.uci.ics.jung.visualization.Layout;
import edu.uci.ics.jung.visualization.PluggableRenderer;
import edu.uci.ics.jung.visualization.SpringLayout;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.ZoomPanGraphMouse;
import edu.uci.ics.jung.visualization.contrib.CircleLayout;
import edu.uci.ics.jung.visualization.contrib.KKLayout;

import orc.runtime.nodes.*;

public class Visualizer {
	private HashMap<Node, NodeVertex> vertices = new HashMap<Node, NodeVertex>();
	private DirectedSparseGraph graph = new DirectedSparseGraph();
	private NodeVertex root;
	private VisualizationViewer viewer;
	
	public Visualizer(Node root) {
		this.root = getVertex(root);
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Graph");
		frame.getContentPane().setLayout(new BorderLayout());
		// render the graph
		PluggableRenderer pr = new PluggableRenderer();
		pr.setEdgeStringer(new EdgeStringer() {
			public String getLabel(ArchetypeEdge edge) {
				return ((NodeEdge)edge).label;
			}
		});
		pr.setVertexStringer(new VertexStringer() {
			public String getLabel(ArchetypeVertex vertex) {
				return ((NodeVertex)vertex).toString();
			}
		});
		pr.setEdgeShapeFunction(new EdgeShape.Line());
		Layout layout = new SpringLayout(graph);
		viewer = new VisualizationViewer(layout, pr);
		layout.advancePositions();
		layout(layout);
		viewer.setGraphMouse(new ZoomPanGraphMouse());
		frame.getContentPane().add(viewer, BorderLayout.CENTER);
		frame.setVisible(true);
	}
	
	public void pick(Node node) {
		viewer.getPickedState().clearPickedVertices();
		viewer.getPickedState().pick(vertices.get(node), true);
	}
	
	private static float spacing = 50;
	private void layout(Layout layout) {
		// initialize levels
		LinkedList<Vertex>[] levels = new LinkedList[findMaxDepth(root)+1];
		for (int i = 0; i < levels.length; ++i) levels[i] = new LinkedList<Vertex>();
		buildLevels(levels, root, 0);
		double y = 0;
		for (LinkedList<Vertex> vertices : levels) {
			double x = vertices.size()*spacing/2;
			for (Vertex vertex : vertices) {
				layout.forceMove(vertex, x, y);
				layout.lockVertex(vertex);
				x -= spacing;
			}
			y += spacing;
		}
	}
	private static int findMaxDepth(Vertex parent) {
		int max = 0;
		for (Vertex child : (Set<Vertex>)parent.getSuccessors()) {
			int depth2 = findMaxDepth(child);
			if (depth2 > max) max = depth2;
		}
		return max+1;
	}
	private void buildLevels(LinkedList<Vertex>[] levels, Vertex parent, int depth) {
		if (!levels[depth].contains(parent)) levels[depth].add(parent);
		for (Vertex child : (Set<Vertex>)parent.getSuccessors()) {
			buildLevels(levels, child, depth+1);
		}
	}
	
	private static class NodeEdge extends DirectedSparseEdge {
		public String label;
		public NodeEdge(NodeVertex arg0, NodeVertex arg1, String label) {
			super(arg0, arg1);
			this.label = label;
		}
	}
	
	private static class NodeVertex extends DirectedSparseVertex {
		public Node node;
		public NodeVertex(Node node) {
			this.node = node;
		}
		public String toString() {
			return node.toString();
		}
	}
	
	private final Visitor<NodeVertex> MAKE_VERTEX = new Visitor<NodeVertex>() {
		public NodeVertex visit(Node node) {
			return newVertex(node);
		}
	
		public NodeVertex visit(Assign node) {
			NodeVertex out = newVertex(node);
			newEdge(out, getVertex(node.next), "next");
			return out;
		}
	
		public NodeVertex visit(Call node) {
			NodeVertex out = newVertex(node);
			newEdge(out, getVertex(node.next), "next");
			return out;
		}
	
		public NodeVertex visit(Defs node) {
			NodeVertex out = newVertex(node);
			int i = 0;
			for (Def def : node.defs) {
				newEdge(out, getVertex(def.body), "def#"+i+"(" + def.arity +")");
				++i;
			}
			newEdge(out, getVertex(node.next), "next");
			return out;
		}
	
		public NodeVertex visit(Fork node) {
			NodeVertex out = newVertex(node);
			newEdge(out, getVertex(node.left), "left");
			newEdge(out, getVertex(node.right), "right");
			return out;
		}
	
		public NodeVertex visit(Leave node) {
			NodeVertex out = newVertex(node);
			newEdge(out, getVertex(node.next), "next");
			return out;
		}
	
		public NodeVertex visit(Let node) {
			NodeVertex out = newVertex(node);
			newEdge(out, getVertex(node.next), "next");
			return out;
		}
	
		public NodeVertex visit(Pub node) {
			return newVertex(node);
		}
	
		public NodeVertex visit(Return node) {
			return newVertex(node);
		}
	
		public NodeVertex visit(Semi node) {
			NodeVertex out = newVertex(node);
			newEdge(out, getVertex(node.left), "left");
			newEdge(out, getVertex(node.right), "right");
			return out;
		}
	
		public NodeVertex visit(Silent node) {
			return newVertex(node);
		}
	
		public NodeVertex visit(Store node) {
			return newVertex(node);
		}
	
		public NodeVertex visit(Subgoal node) {
			NodeVertex out = newVertex(node);
			newEdge(out, getVertex(node.left), "left");
			newEdge(out, getVertex(node.right), "right");
			return out;
		}
	
		public NodeVertex visit(Unwind node) {
			NodeVertex out = newVertex(node);
			newEdge(out, getVertex(node.next), "next");
			return out;
		}
	
		public NodeVertex visit(WithLocation node) {
			return getVertex(node.next);
		}
	};
	
	private NodeVertex getVertex(Node node) {
		NodeVertex out =  vertices.get(node);
		if (out == null) {
			out = node.accept(MAKE_VERTEX);
			vertices.put(node, out);
		}
		return out;
	}
	
	private NodeVertex newVertex(Node node) {
		NodeVertex out = new NodeVertex(node);
		graph.addVertex(out);
		return out;
	}
	
	private Edge newEdge(NodeVertex a, NodeVertex b, String label) {
		return graph.addEdge(new NodeEdge(a,b,label));
	}
}

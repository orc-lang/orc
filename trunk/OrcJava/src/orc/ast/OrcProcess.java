/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.ast;

import orc.runtime.nodes.Node;
import orc.runtime.nodes.Param;
import java.util.*;

/**
 * Base class for the abstract syntax tree
 * @author wcook
 */
abstract public class OrcProcess {

	/**
	 * Compiles abstrac syntax tree into execution nodes.
	 * Every node is compile relative to an "output" node that represents
	 * the "rest of the program". Thus the tree of compiled nodes is created bottom up.
	 * @param output IMPORTANT: this is the node to which output will be directed
	 * @return A new node
	 */
	public abstract Node compile(Node output, List<orc.ast.Definition> defs);
	
	/*
	 * In order to translate expressions with nested calls, we need to know which names
	 * are definitions, which are formal parameters, and which are values.
	 * @param bound -- strings from context that are bound to formal parameters.
	 * @param vals -- string from context that are bound to values (by ">x>" or "where x in")
	 * @return -- Replace all Names in the process by Variable,Value,or Def
	 */
	public abstract OrcProcess resolveNames(List<String> bound, List<String> vals);

	public Param asParam() {
		return null; // overriden by parameter types
	}
	public boolean isSimple() {
		return false; // overriden by name types
	}
	public boolean isValue() {
		return false; // overriden by Variable
	}
	public boolean isDef(List<orc.ast.Definition> defs) {
		return false; // overriden by Def
	}
	public orc.ast.Definition Lookup(List<orc.ast.Definition> defs) {
		return null; // overriden by Def
	}
	public String Name() {
		return null; // overriden by name types
	}
	public OrcProcess addDefs(OrcProcess p) {
	 return p;// overriden by Define 
	}
}

/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import java.util.List;
import java.io.*;

/**
 * Compiled node to create a definition
 * @author wcook
 */
public class Definition implements Serializable {
	private static final long serialVersionUID = 1L;
	public String name;
	public List<String> formals;
	public Node body;

	public Definition(String name, List<String> formals, Node body) {
		this.name = name;
		this.formals = formals;
		this.body = body;
	}
}

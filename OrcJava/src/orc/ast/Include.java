package orc.ast;

import orc.runtime.nodes.Node;

public class Include extends OrcProcess{
	
	OrcProcess file;
    OrcProcess expr;

    public Include(OrcProcess file, OrcProcess expr) {
		this.file=file;
		this.expr=expr;
	}
    public Node compile(Node output) {
    	return file.compile(expr.compile(output));
    }

}

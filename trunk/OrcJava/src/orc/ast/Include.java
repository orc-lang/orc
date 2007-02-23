package orc.ast;

import java.util.*;

import orc.runtime.nodes.Node;

public class Include extends OrcProcess{
	
	OrcProcess file;
    OrcProcess expr;

    public Include(OrcProcess file, OrcProcess expr) {
		this.file=file;
		this.expr=expr;
	}
    
    /** 
	 * To resolve names in an include, just resolve both parts.
	 */
	public OrcProcess resolveNames(List<String> bound, List<String> vals){
		return new Include(file.resolveNames(bound,vals),
				           expr.resolveNames(bound,vals));
	}
    public Node compile(Node output,List<orc.ast.Definition> defs) {
    	//The included file adds its defs to the expression, and then we compile.
    	return file.addDefs(expr).compile(output,defs);
    }
    
    /*  In the case of nested imports, both file and expr may have definitions to add.
     */
    public OrcProcess addDefs(OrcProcess p) {
		 return file.addDefs(expr.addDefs(p));
		}

}

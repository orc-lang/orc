package orc.orcx;

import java.util.LinkedList;
import java.util.List;

import orc.ast.simple.Call;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.FreeVar;

/**
 * Node in the extended AST for an embedded XQuery. 
 * 
 * Simplification will convert this to a form of site call.
 * 
 * @author dkitchin, kmorton
 *
 */
public class EmbeddedXQuery extends orc.ast.extended.Expression {

	
	List<String> freevars;
	int queryid;
	OrcX owner;
	
	public EmbeddedXQuery(OrcX owner, int queryid, List<String> freevars)
	{
		this.queryid = queryid;  
		this.freevars = freevars;
		this.owner = owner;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() {
		
		List<Argument> args = new LinkedList<Argument>();
		List<String> names = new LinkedList<String>();

		// Create an argument list of free vars using the query's free vars as names
		for (String s : freevars) {
			args.add(new FreeVar(s));
			names.add(s);
		}
		
		return new Call(new orc.ast.simple.arg.Site(new OrcXSite(queryid, owner)), args);
	}

}

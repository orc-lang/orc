package orc.ast.simple.type;

import java.util.List;

import orc.env.Env;

/**
 * 
 * A syntactic type encompassing all of the information associated with a datatype.
 * 
 * @author dkitchin
 *
 */
public class Datatype extends Type {

	public String typename;
	public List<Constructor> members;
	public List<VariantTypeFormal> formals;
	
	public Datatype(String typename, List<Constructor> members, List<VariantTypeFormal> formals) {
		this.typename = typename;
		this.members = members;
		this.formals = formals;
	}

	@Override
	public orc.type.Type convert(Env<String> env) {
		// TODO: Instrument datatypes correctly in the typechecker 
		return null;
	}

}

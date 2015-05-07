package orc.lib.state.types;

import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.DotType;
import orc.type.structured.ListType;
import orc.type.tycon.MutableContainerType;

public class RefType extends MutableContainerType {

	public String toString() {
		return "Ref";
	}
	
	public Type makeCallableInstance(List<Type> params) throws TypeException {
		Type T = params.get(0);
		
		DotType dt = new DotType(/* no default behavior */);
		dt.addField("read", new ArrowType(T));
		dt.addField("readnb", new ArrowType(T));
		dt.addField("write", new ArrowType(T, Type.TOP));
		return dt;
	}
	
}

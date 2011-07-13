package orc.lib.state.types;

import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.type.ArrowType;
import orc.type.DotType;
import orc.type.ListType;
import orc.type.MutableContainerType;
import orc.type.Type;

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
package orc.lib.state.types;

import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.DotType;
import orc.type.structured.ListType;
import orc.type.tycon.MutableContainerType;

public class SyncChannelType extends MutableContainerType {

	public String toString() {
		return "SyncChannel";
	}
	
	public Type makeCallableInstance(List<Type> params) throws TypeException {
		/* We know that SyncChannel has exactly one type parameter */
		Type T = params.get(0);
		
		DotType dt = new DotType(/* no default behavior */);
		dt.addField("get", new ArrowType(T));
		dt.addField("put", new ArrowType(T, Type.TOP));
		return dt;
	}
	
}

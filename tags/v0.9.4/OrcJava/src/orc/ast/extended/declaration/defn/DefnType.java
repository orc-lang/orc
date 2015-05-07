package orc.ast.extended.declaration.defn;

import java.util.List;

import orc.ast.extended.Expression;
import orc.ast.extended.pattern.Pattern;
import orc.type.ArrowType;
import orc.type.Type;

public class DefnType extends Defn {

		public List<Type> argTypes;
		public Type resultType;
	
		public DefnType(String name, List<Type> argTypes, Type resultType) {
			this.name = name;
			this.argTypes = argTypes;
			this.resultType = resultType;
		}

		public String toString() {
			return "def " + name + " (" + Expression.join(argTypes, ", ") + ") :: " + resultType;
		}

		@Override
		public void extend(AggregateDefn adef) {
			adef.setType(new ArrowType(argTypes, resultType));
		}	
	
}

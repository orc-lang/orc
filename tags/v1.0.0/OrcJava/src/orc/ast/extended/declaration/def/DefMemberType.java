package orc.ast.extended.declaration.def;

import java.util.List;

import orc.ast.extended.Visitor;
import orc.ast.extended.expression.Expression;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.type.LambdaType;
import orc.ast.extended.type.Type;
import orc.error.compiletime.CompilationException;

/**
 * 
 * A declaration of the form:
 *
 * def f[T,...,T](T,...,T)(T,...,T)... :: T
 * 
 * This declares the signature of the function f. Type parameters [T,...,T]
 * may be an empty list. There may be one or more argument type groups (T,...,T).
 *
 * @author dkitchin
 */
public class DefMemberType extends DefMember {

		public List<List<Type>> argTypesList; // Must not be empty
		public Type resultType; // May be null
		public List<String> typeParams; // May be empty if there are no type parameters
	
		public DefMemberType(String name, List<List<Type>> argTypesList, Type resultType, List<String> typeParams) {
			this.name = name;
			this.argTypesList = argTypesList;
			this.resultType = resultType;
			this.typeParams = typeParams;
		}

		public String sigToString() {
			StringBuilder s = new StringBuilder();
			
			s.append(name);
			
			s.append('[');
				s.append(Expression.join(typeParams, ", "));
			s.append(']');
			
			for (List<Type> argTypes : argTypesList) {
				s.append('(');	
					s.append(Expression.join(argTypes, ","));
				s.append(')');
			}
				
			s.append(')');
			s.append(" :: ");
			s.append(resultType);

			return s.toString();
		}
		
		public String toString() {
			return "def " + sigToString();
		}

		/* (non-Javadoc)
		 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
		 */
		public <E> E accept(Visitor<E> visitor) {
			return visitor.visit(this);
		}

		@Override
		public void extend(AggregateDef adef) throws CompilationException {
			
			LambdaType lt = (new LambdaType(argTypesList, resultType, typeParams)).uncurry();
			
			adef.setTypeParams(lt.typeParams);
			adef.setArgTypes(lt.argTypes.get(0));
			adef.setResultType(lt.resultType);
			adef.addLocation(getSourceLocation());
		}	
	
}

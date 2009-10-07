package orc.lib.state;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import orc.ast.oil.expression.argument.Argument;
import orc.ast.oil.expression.argument.Constant;
import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.type.Type;
import orc.type.TypingContext;
import orc.type.structured.DotType;

/**
 * @author quark
 */
public class Record extends EvalSite {
	private static class RecordInstance extends EvalSite {
		private HashMap<String, Object> map = new HashMap<String, Object>();
		@Override
		public Object evaluate(Args args) throws TokenException {
			String field = args.fieldName();
			return map.get(field);
		}
		
		public String toString() {
			return map.toString();
		}
		
		private void put(String key, Object value) {
			map.put(key, value);
		}
	}

	@Override
	public Object evaluate(Args args) throws TokenException {
		RecordInstance out = new RecordInstance();
		Iterator<Object> argsi = args.iterator();
		while (argsi.hasNext()) {
			Object keyo = argsi.next();
			String key;
			try {
				key = (String)keyo;
			} catch (ClassCastException e) {
				throw new ArgumentTypeMismatchException(e);
			}
			if (!argsi.hasNext()) {
				throw new ArityMismatchException("Record key missing a value");
			}
			out.put(key, argsi.next());
		}
		return out;
	}

	private static class RecordBuilderType extends Type {
	
		/* Override the default type call implementation,
		 * using the string values of the args directly
		 * within the constructed dot type.
		 */
		public Type call(TypingContext ctx, List<Argument> args, List<Type> typeActuals) throws TypeException {
			
			int i = 0;
			DotType dt = new DotType();
			String key = "";
			try {
				while (i < args.size()) {
					Constant c = (Constant)args.get(i);
					key = (String)c.v;
					Type t = args.get(i+1).typesynth(ctx);
					dt.addField(key, t);
					i += 2;
				}
			}
			catch (ClassCastException e) {
				throw new TypeException("Record field name at index " + i + " must be a string constant");
			}
			catch (IndexOutOfBoundsException e) {
				throw new TypeException("Arity mismatch: no initial value given after field name '" + key + "'");
			}
			
			return dt;
		}
		
	}
	private static Type thisType = new RecordBuilderType();
	
	public Type type() {
		return thisType;
	}
}

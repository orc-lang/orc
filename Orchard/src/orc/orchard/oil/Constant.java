package orc.orchard.oil;

public class Constant extends Value {
	public Object value;
	public Constant() {}
	public Constant(Object value) {
		this.value = value;
	}
	public String toString() {
		return super.toString() + "(" + value.getClass().toString() + "(" + value + "))";
	}
	@Override
	public orc.ast.oil.arg.Arg unmarshal() {
		orc.ast.val.Val val;
		if (value instanceof Integer) {
			val = new orc.ast.val.Int((Integer)value);
		} else if (value instanceof String) {
			val = new orc.ast.val.Str((String)value);
		} else if (value instanceof Boolean) {
			val = new orc.ast.val.Bool((Boolean)value);
		} else {
			throw new AssertionError("Unexpected constant of type " + value.getClass().toString());
		}
		return new orc.ast.oil.arg.Constant(val);
	}
}

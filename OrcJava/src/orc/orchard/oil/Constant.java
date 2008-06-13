package orc.orchard.oil;

import java.math.BigInteger;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlTransient;

public class Constant extends Argument {
	public Object value;
	public Constant() {}
	public Constant(Object value) {
		this.value = value;
	}
	public String toString() {
		return super.toString() + "(" + value + ")";
	}
	@Override
	public orc.ast.oil.arg.Arg unmarshal() {
		orc.val.Val val;
		if (value instanceof Integer) {
			val = new orc.val.Int((Integer)value);
		} else if (value instanceof String) {
			val = new orc.val.Str((String)value);
		} else if (value instanceof Boolean) {
			val = new orc.val.Bool((Boolean)value);
		} else {
			throw new AssertionError("Unexpected constant of type " + value.getClass().toString());
		}
		return new orc.ast.oil.arg.Constant(val);
	}
}

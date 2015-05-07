package orc.type;

public class ConstIntType extends NumberType {

	Integer i;
	
	public ConstIntType(Integer i) {
		this.i = i;
	}

	/* We use the Java inheritance hierarchy as a default */
	public boolean subtype(Type that) {
		
		if (that instanceof ConstIntType) {
			
			ConstIntType other = (ConstIntType)that;
			
			return (i == other.i);
		}
		else {
			return Type.INTEGER.subtype(that);
		}
	}

	public Type join(Type that) {
		if (that instanceof ConstIntType) {	
			ConstIntType other = (ConstIntType)that;
			if (i == other.i) {
				return this;
			}
			else {
				return Type.INTEGER;
			}
		}
		else {
			return super.join(that);
		}
		
		
	}
	
	public String toString() { return "integer(=" + i +")"; }
	
}

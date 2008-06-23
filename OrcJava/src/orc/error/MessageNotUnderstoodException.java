package orc.error;

public class MessageNotUnderstoodException extends TokenException {

	String field;
		
	public MessageNotUnderstoodException(String field) {
		super("The message " + field + " was not understood by this site.");
		this.field = field;
	}

}

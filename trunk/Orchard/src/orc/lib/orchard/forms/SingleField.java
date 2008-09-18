package orc.lib.orchard.forms;

import java.util.List;


public abstract class SingleField<V> extends Field<V> {
	protected String posted;
	@SuppressWarnings("serial")
	public static class ValidationException extends Exception {
		private String message;
		public ValidationException(String message) {
			this.message = message;
		}
		public String getMessage() {
			return message;
		}
	}
	
	public SingleField(String key, String label, String posted) {
		super(key, label, null);
		this.posted = posted;
	}

	public void readRequest(FormData request, List<String> errors) {
		try {
			posted = request.getParameter(key);
			value = requestToValue(posted);
		} catch (ValidationException e) {
			errors.add(e.getMessage());
		}
	}
	
	public abstract V requestToValue(String posted) throws ValidationException;
}

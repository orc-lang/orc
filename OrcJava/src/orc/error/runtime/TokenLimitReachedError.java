package orc.error.runtime;

public class TokenLimitReachedError extends TokenError {
	public TokenLimitReachedError(int limit) {
		super("Token limit (limit=" + limit + ") reached");
	}
}

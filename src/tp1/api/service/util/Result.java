package tp1.api.service.util;


import jakarta.ws.rs.core.Response;

/**
 *
 * Represents the result of an operation, either wrapping a result of the given type,
 * or an error.
 *
 * @author smd
 *
 * @param <T> type of the result value associated with success
 */
public interface Result<T> {

	enum ErrorCode{ OK, CONFLICT, NOT_FOUND, BAD_REQUEST, FORBIDDEN, INTERNAL_ERROR, NOT_IMPLEMENTED, NOT_AVAILABLE };

	static ErrorCode mapError(Response.Status status) {
		return switch (status) {
			case CONFLICT -> ErrorCode.CONFLICT;
			case NOT_FOUND -> ErrorCode.NOT_FOUND;
			case BAD_REQUEST -> ErrorCode.BAD_REQUEST;
			case FORBIDDEN -> ErrorCode.FORBIDDEN;
			case INTERNAL_SERVER_ERROR -> ErrorCode.INTERNAL_ERROR;
			case NOT_IMPLEMENTED -> ErrorCode.NOT_IMPLEMENTED;
			default -> ErrorCode.BAD_REQUEST;
		};
	}

	static ErrorCode mapError(String msg) {
		if(msg.contains("CONFLICT")) return ErrorCode.CONFLICT;
		if(msg.contains("NOT_FOUND")) return ErrorCode.NOT_FOUND;
		if(msg.contains("BAD_REQUEST")) return ErrorCode.BAD_REQUEST;
		if(msg.contains("FORBIDDEN")) return ErrorCode.FORBIDDEN;
		if(msg.contains("INTERNAL_SERVER_ERROR")) return ErrorCode.INTERNAL_ERROR;
		if(msg.contains("NOT_IMPLEMENTED")) return ErrorCode.NOT_IMPLEMENTED;

		return ErrorCode.BAD_REQUEST;
	}

	/**
	 * Tests if the result is an error.
	 */
	boolean isOK();

	/**
	 * obtains the payload value of this result
	 * @return the value of this result.
	 */
	T value() throws Exception;

	/**
	 *
	 * obtains the error code of this result
	 * @return the error code
	 *
	 */
	Result.ErrorCode error();

	String toString();

	/**
	 * Convenience method for returning non error results of the given type
	 * @return the value of the result
	 */
	static <T> Result<T> ok( T result ) {
		return new OkResult<>(result);
	}

	/**
	 * Convenience method for returning non error results without a value
	 * @return non-error result
	 */
	static <T> OkResult<T> ok() {
		return new OkResult<>(null);
	}

	/**
	 * Convenience method used to return an error
	 * @return
	 */
	static <T> ErrorResult<T> error(ErrorCode code, Exception exception) {
		return new ErrorResult<>(code, exception);
	}

	static <T> ErrorResult<T> error(Response.Status code, Exception exception) {
		return new ErrorResult<>(Result.mapError(code), exception);
	}

	static <T> ErrorResult<T> error(String msg, Exception exception) {
		return new ErrorResult<>(Result.mapError(msg), exception);
	}

}

class OkResult<T> implements Result<T> {

	final T result;

	OkResult(T result) {
		this.result = result;
	}

	@Override
	public boolean isOK() {
		return true;
	}

	@Override
	public T value() {
		return result;
	}

	public ErrorCode error() {
		return ErrorCode.OK;
	}

	public String toString() {
		return "(OK, " + value().toString() + ")";
	}
}

class ErrorResult<T> implements Result<T> {

	final ErrorCode code;
	final Exception exception;

	ErrorResult(ErrorCode code, Exception exception) {
		this.code = code;
		this.exception = exception;
	}

	@Override
	public boolean isOK() {
		return false;
	}

	@Override
	public T value() throws Exception {
		throw exception;
	}

	@Override
	public ErrorCode error() {
		return code;
	}

	public String toString() {
		return "(" + error() + ")";
	}
}
package tp1.api.service.util;


import jakarta.ws.rs.core.Response;

import java.util.Map;

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
			case SERVICE_UNAVAILABLE -> ErrorCode.NOT_AVAILABLE;
			default -> ErrorCode.BAD_REQUEST;
		};
	}

	static Response.Status mapError(ErrorCode status) {
		return switch (status) {
			case CONFLICT -> Response.Status.CONFLICT;
			case NOT_FOUND -> Response.Status.NOT_FOUND;
			case BAD_REQUEST -> Response.Status.BAD_REQUEST;
			case FORBIDDEN -> Response.Status.FORBIDDEN;
			case INTERNAL_ERROR -> Response.Status.INTERNAL_SERVER_ERROR;
			case NOT_IMPLEMENTED -> Response.Status.NOT_IMPLEMENTED;
			case NOT_AVAILABLE -> Response.Status.SERVICE_UNAVAILABLE;
			default -> Response.Status.BAD_REQUEST;
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
	T value();

	/**
	 *
	 * obtains the error code of this result
	 * @return the error code
	 *
	 */
	Result.ErrorCode error();

	Map<String, String> getOthers();

	void setOthers(Map<String, String> others);

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

	/**
	 * Convenience method used to return an error
	 * @return
	 */
	static <T> ErrorResult<T> error(ErrorCode code) {
		return new ErrorResult<>(code, new Exception(code.name()));
	}

	static <T> ErrorResult<T> error(Response.Status code, Exception exception) {
		return new ErrorResult<>(Result.mapError(code), exception);
	}

	static <T> ErrorResult<T> error(Response.Status code) {
		return new ErrorResult<>(Result.mapError(code), new Exception(code.name()));
	}

	static <T> ErrorResult<T> error(String msg, Exception exception) {
		return new ErrorResult<>(Result.mapError(msg), exception);
	}

}

class OkResult<T> implements Result<T> {

	final T result;
	Map<String, String> others;

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

	@Override
	public ErrorCode error() {
		return ErrorCode.OK;
	}

	@Override
	public Map<String, String> getOthers() {
		return others;
	}

	@Override
	public void setOthers(Map<String, String> others) {
		this.others = others;
	}

	@Override
	public String toString() {
		return "OkResult{" +
				"result=" + result +
				", others=" + others +
				'}';
	}
}

class ErrorResult<T> implements Result<T> {

	final ErrorCode code;
	final Exception exception;
	Map<String, String> others;

	ErrorResult(ErrorCode code, Exception exception) {
		this.code = code;
		this.exception = exception;
	}

	@Override
	public boolean isOK() {
		return false;
	}

	@Override
	public T value() {
		return null;
	}

	@Override
	public ErrorCode error() {
		return code;
	}

	@Override
	public Map<String, String> getOthers() {
		return others;
	}

	@Override
	public void setOthers(Map<String, String> others) {
		this.others = others;
	}

	@Override
	public String toString() {
		return "ErrorResult{" +
				"code=" + code +
				", exception=" + exception.getMessage() +
				", others=" + others +
				'}';
	}
}
package com.matburt.mobileorg;

public class ReportableError extends Throwable {
	private String message;
	private Throwable originalError;
	
	public ReportableError(String message, Throwable originalError) {
		this.message = message;
		this.originalError = originalError;
	}
	
	public String getMessage() {
		return this.message;
	}
	
	public Throwable getOriginalError() {
		return this.originalError;
	}
}

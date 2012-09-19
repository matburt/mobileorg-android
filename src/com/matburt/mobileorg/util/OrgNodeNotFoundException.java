package com.matburt.mobileorg.util;

public class OrgNodeNotFoundException extends Exception {

	private static final long serialVersionUID = 6603637490966826497L;

	public OrgNodeNotFoundException() {
		super();
	}
	
	public OrgNodeNotFoundException(String message) {
		super(message);
	}

	public OrgNodeNotFoundException(OrgFileNotFoundException e) {
		super(e);
	}
}

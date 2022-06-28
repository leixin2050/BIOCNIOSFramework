package com.lw.nio.action;

public interface IRequestResponseDealer {
	String dealRequest(String action, String argument) throws Exception;
	void dealResponse(String action, String argument) throws Exception;
}

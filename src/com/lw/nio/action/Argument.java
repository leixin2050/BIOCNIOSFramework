package com.lw.nio.action;

import java.lang.reflect.Parameter;

public class Argument {
	private Parameter parameter;
	private Class<?> parameterType;
	private String name;
	
	public Argument() {
	}

	public Parameter getParameter() {
		return parameter;
	}

	public void setParameter(Parameter parameter) {
		this.parameter = parameter;
	}

	public Class<?> getParameterType() {
		return parameterType;
	}

	public void setParameterType(Class<?> parameterType) {
		this.parameterType = parameterType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}

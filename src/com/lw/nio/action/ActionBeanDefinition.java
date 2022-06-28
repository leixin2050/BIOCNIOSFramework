package com.lw.nio.action;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ActionBeanDefinition {
	private String action;
	private Class<?> klass;
	private Object object;
	private Method method;
	private List<Argument> parameterList;
	
	public ActionBeanDefinition() {
		this.parameterList = new ArrayList<Argument>();
	}
	
	public List<Argument> getParameterList() {
		return parameterList;
	}

	public Class<?>[] getParameterTypes() {
		if (this.parameterList.isEmpty()) {
			return new Class<?>[] {};
		}
		int parameterCount = this.parameterList.size();
		Class<?>[] parameterTypes = new Class<?>[parameterCount];
		for (int i = 0; i < parameterCount; i++) {
			Argument arg = this.parameterList.get(i);
			parameterTypes[i] = arg.getParameterType();
		}
		
		return parameterTypes;
	}
	
	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public Class<?> getKlass() {
		return klass;
	}

	public void setKlass(Class<?> klass) {
		this.klass = klass;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}
	
	public void addParameter(Argument arg) {
		this.parameterList.add(arg);
	}

	@Override
	public String toString() {
		return "ActionBeanDefinition [action=" + action + ", klass=" + klass + ", method=" + method + ", parameterList="
				+ parameterList + "]";
	}
	
}

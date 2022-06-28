package com.lw.nio.action;

import util.ArgumentMaker;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.List;


public class DefaultRequestResponseDealer implements IRequestResponseDealer {

	public DefaultRequestResponseDealer() {
	}
	
	@Override
	public String dealRequest(String action, String argument) throws Exception {
		ActionBeanDefinition definition = ActionBeanFactory.getActionBean(action);

		if (definition == null) {
			throw new Exception("action" + action + "不存在！");
		}
		
		Object object = definition.getObject();
		Method method = definition.getMethod();
		
		Object[] args = getArgs(definition, argument);
		Object result = method.invoke(object, args);
		
		if(method.getReturnType().equals(void.class)) {
			return null;
		}
		return ArgumentMaker.gson.toJson(result);
	}
	
	private Object[] getArgs(ActionBeanDefinition definition, String string) {
		ArgumentMaker maker = new ArgumentMaker(string);
		int argCount = maker.getArgumentCount();
		
		if (argCount <= 0) {
			return new Object[] {};
		}
		
		Object[] args = new Object[argCount];
		int index = 0;
		List<Argument> argList = definition.getParameterList();
		for (Argument arg : argList) {
			String argName = arg.getName();
			Type type = arg.getParameter().getParameterizedType();
			args[index++] = maker.getArg(argName, type);
		}
		
		return args;
	}

	@Override
	public void dealResponse(String action, String argument) throws Exception {
		ActionBeanDefinition definition = ActionBeanFactory.getActionBean(action);
		
		if (definition == null) {
			throw new Exception("action[" + action + "] 不存在！");
		}
		Object object = definition.getObject();
		Method method = definition.getMethod();
		
		Parameter parameter = method.getParameters()[0];
		Type type = parameter.getParameterizedType();
		Object arg = ArgumentMaker.gson.fromJson(argument, type);
		
		method.invoke(object, arg);
	}

}

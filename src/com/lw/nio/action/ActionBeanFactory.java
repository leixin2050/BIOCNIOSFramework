package com.lw.nio.action;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import util.PackageScanner;
import util.TypeParser;
import util.XMLParser;

public class ActionBeanFactory {
	private static final Map<String, ActionBeanDefinition> actionBeanPool
			= new HashMap<String, ActionBeanDefinition>();
	
	public ActionBeanFactory() {
	}
	
	public static void scanMappingByAnnotation(String packageName) throws ClassNotFoundException, URISyntaxException, IOException {
		new PackageScanner() {
			@Override
			public void dealClass(Class<?> klass) {
				if (!klass.isAnnotationPresent(ActionClass.class)) {
					return;
				}
				try {
					Object object = klass.newInstance();
					Method[] methods = klass.getDeclaredMethods();
					for (Method method : methods) {
						if (!method.isAnnotationPresent(Action.class)) {
							continue;
						}
						//取得行为的名字
						Action action = method.getAnnotation(Action.class);
						String actionName = action.action();
						
						ActionBeanDefinition abd = new ActionBeanDefinition();
						abd.setAction(actionName);
						abd.setKlass(klass);
						abd.setObject(object);
						abd.setMethod(method);
						dealParameter(abd, method);
						
						actionBeanPool.put(actionName, abd);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.scanPackage(packageName);
	}
	
	private static void dealParameter(ActionBeanDefinition abd, Method method) throws Exception {
		Parameter[] parameters = method.getParameters();
		int index = 0;
		for (Parameter parameter : parameters) {
			if (!parameter.isAnnotationPresent(Para.class)) {
				throw new Exception("方法[" + method.getName() + "]的第" + (index + 1) + "个参数没有Para注解！");
			}

			//这里解决了反射机制执行时会将形参名擦除的问题，提供一个注解@Para(value=name)
			Para para = parameter.getAnnotation(Para.class);
			String paraName = para.value();
			
			Argument arg = new Argument();
			arg.setName(paraName);
			arg.setParameter(parameter);
			arg.setParameterType(parameter.getType());
			abd.addParameter(arg);
			index++;
		}
	}
	
	public static void scanMappingByXml(String mappingPath) throws Exception {
		new XMLParser() {
			@Override
			public void dealElement(Element element, int index) throws Exception {
				String action = element.getAttribute("name");
				String className = element.getAttribute("class");
				Class<?> klass = Class.forName(className);
				Object object = klass.newInstance();
				
				ActionBeanDefinition definition = new ActionBeanDefinition();
				definition.setAction(action);
				definition.setKlass(klass);
				definition.setObject(object);
				
				new XMLParser() {
					@Override
					public void dealElement(Element element, int index) throws Exception {
						String methodName = element.getAttribute("name");
						new XMLParser() {
							@Override
							public void dealElement(Element element, int index) throws Exception {
								String strType = element.getAttribute("type");
								String paraName = element.getAttribute("name");
								
								Argument argument = new Argument();
								argument.setName(paraName);
								argument.setParameterType(TypeParser.strToClass(strType));
								
								definition.addParameter(argument);
							}
						}.parse(element, "parameter");
						
						Class<?>[] parameterTypes = definition.getParameterTypes();
						Method method = klass.getDeclaredMethod(methodName, parameterTypes);
						definition.setMethod(method);
					}
				}.parse(element, "method");
				
				actionBeanPool.put(action, definition);
			}
		}.parse(XMLParser.getDocument(mappingPath), "action");
	}
	
	public static void setActionObject(String action, Object object) throws Exception{
		ActionBeanDefinition actionBeanDefinition = actionBeanPool.get(action);
		if (actionBeanDefinition == null) {
			throw new Exception("action【" + action + "】不存在");
		}
		actionBeanDefinition.setObject(object);
	}
	
	public static ActionBeanDefinition getActionBean(String action) {
		return actionBeanPool.get(action);
	}
}

package com.lw.nio.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author leiWei
 * 接收到信息后的自动匹配对应信令的处理方法，别反射自动执行该方法，对于服务器与客户端均适用
 * */
public class NetMessageDealer {

    public NetMessageDealer() {
    }

    /**
     * @param object 处理消息的对象
     * @param netMessage 接收到的信息
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    static void dealMessage(Object object, NetMessage netMessage) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ENetCommond commond = netMessage.getCommond();
        //通过Command匹配对应的deal处理方法
        String methodName = commandToMethodName(commond);

        Class<?> klass = object.getClass();
        Method method = klass.getDeclaredMethod(methodName, NetMessage.class);
        //反射调用处理方法
        method.invoke(object, netMessage);
    }

    /**
     * 转换信令为方法名
     * @param commond 信令
     * @return 规定处理消息的方法名为deal + 信令的驼峰格式
     */
    private static String commandToMethodName(ENetCommond commond) {
        //TO_ONE
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("deal");
        String str = commond.name();
        String[] coms = str.split("_");
        for(String com : coms) {
            com = com.substring(0,1) + com.substring(1).toLowerCase();
            stringBuffer.append(com);
        }
        return stringBuffer.toString();
    }

}

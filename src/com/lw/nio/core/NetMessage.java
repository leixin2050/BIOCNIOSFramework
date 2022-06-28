package com.lw.nio.core;

import sun.nio.ch.Net;

import java.math.BigDecimal;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author leiWei
 *
 * 自定义协议类
 * 我们所传输的信息肯定不可以杂乱无章，这样无论是发送端编码还是接收端解码都是一种无奈的事情
 * 所以模拟tcp头，我对于传输的信息所进行协议的制定
 * 协议的三要素为：
     * 语法，即用来规定信息的格式；
     * 语义，即用来说明通信双方应当怎么做；
     * 时序，即详细说明事件的先后顺序。
 * 信息为NetMessage：
 *   信息头：
 *      1、(枚举类)NetCommond：传输的命令，即标识此次消息的信令
 *      2、String action 分发器命令
 *      3、int type 发送消息的内容是什么类型的
 *      4、int length 发送消息的长度
 *
 *      5、byte[] message传输的内容
 */
public class NetMessage {
    //信息类型
    public static final int BINARY = 0;
    public static final int STRING = 1;

    //信令
    private ENetCommond commond;
    //传输信息的种类
    private int type;
    //信息长度
    private int length;
    //分发器的实现
    private String action;

    //信息体
    private byte[] message;


    public NetMessage() {
        this.length = 0;
    }

    //对消息的头部的解析
    public NetMessage(String mess) {
        String[] strs = mess.split(":");
        this.commond = ENetCommond.valueOf(strs[0]);
        this.action = strs[1];
        this.length = Integer.valueOf(strs[2]);
        this.type = Integer.valueOf(strs[3]);
    }

    ENetCommond getCommond() {
        return commond;
    }

    NetMessage setCommond(ENetCommond commond) {
        this.commond = commond;
        return this;
    }

    int getType() {
        return type;
    }
    //type不允许设置，在设置消息时自动产生

    String getAction() {
        return action;
    }

    NetMessage setAction(String action) {
        this.action = action;
        return this;
    }

    public int getLength() {
        return length;
    }

    byte[] getbyte() {
        return message;
    }

    /**
     * 设置binary类型的消息
     * @param message
     * @return
     */
    NetMessage setMessage(byte[] message) {
        this.type = BINARY;
        this.length = message.length;
        this.message = message;
        return this;
    }

    /**
     * 设置String类型的消息
     * @param message
     * @return
     */
    NetMessage setMessage(String message) {
        this.type = STRING;
        this.message = message.getBytes();
        this.length = this.message.length;
        return this;
    }

    /**
     * 根据type参数来设置的消息
     * @param message
     * @param type
     * @return
     */
    NetMessage setMessage(String message, int type) {
        this.type = type;
        this.message = message.getBytes();
        this.length = this.message.length;
        return this;
    }

    String getString() {
        String mess = "";
        if (type == STRING) {
            mess = new String(this.message);
        }
        return mess;
    }

    /**
     * 对消息头的封装
     * 使用toString来进行消息头的封装
     * @return
     */
    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(this.commond).append(":")
                .append(this.action).append((this.action == null || this.action.length() <= 0) ? "" : ":")
                .append(this.length).append(":")
                .append(this.type);
        return stringBuffer.toString();
    }
}

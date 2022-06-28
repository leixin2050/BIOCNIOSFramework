package com.lw.nio.core;

import util.PropertiesParser;

import java.io.IOException;
import java.net.Socket;

/**
 * @author leiWei
 * 供外部使用的客户端
 */
public class Client {
    public static final String DEFAULT_SERVER_IP = "localhost";
    private int port;
    private String ip;

    private ClientConverSation clientConverSation;
    //使用适配器模式来添加客户端行为
    private IClientAction clientAction;

    public Client() {
        this.ip = DEFAULT_SERVER_IP;
        this.port = Server.DEFAULT_SERVER_PORT;
        this.clientAction = new ClientActionAdapter();
    }

    //外部初始化Client后调用connect方法进行与服务器端的连接
    public void connect() throws IOException {
        Socket socket = new Socket(this.ip, this.port);
        this.clientConverSation = new ClientConverSation(this, socket);
        this.clientAction.afterConnect();
    }

    IClientAction getClientAction() {
        return clientAction;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    //支持propergies文件配置客户端关于服务器的ip与port,当未设置时采取默认值
    public void loadConfig(String pathName) {
        PropertiesParser.load(pathName);
        String strValue;
        int intValue;

        try {
            intValue = PropertiesParser.get("server_port", Integer.class);
            if (intValue > 0) {
                setPort(intValue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            strValue = PropertiesParser.get("server_ip", String.class);
            if (strValue != null && strValue.length() > 0) {
                setIp(strValue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /***************外部接口调用的信息*************客户端向其他客户端发送信息******************************/
    public void toOne(String id, String message) {
        this.clientConverSation.toOne(id, message);
    }

    /**
     * 给除自己之外的所有用户发送信息
     * @param id
     * @param message
     */
    public void toOther(String id, String message) {
        this.clientConverSation.toOther(id, message);
    }

    /**
     * 下线操作
     */
    public void offline() {
        if (this.clientAction.ensureOffline()) {
            this.clientAction.beforeOffline();
            this.clientConverSation.offline();
            this.clientAction.afterOffline();
        }
    }

    /**
     * 分发器的请求操作
     * @param action 请求行为，且不需要返回行为
     * @param argument 请求行为的参数，由ArgumentMaker得到的json字符串
     */
    public void request(String action, String argument) {
        this.clientConverSation.request(action, argument);
    }

    /**
     * 存在返回值的分发器处理程序
     * @param requestAction   请求行为
     * @param responseAction  响应行为
     * @param argument
     */
    public void requestString(String requestAction, String responseAction, String argument) {
        this.clientConverSation.request(requestAction, responseAction,argument);
    }
}

package com.lw.nio.core;

import com.lw.nio.action.ActionBeanDefinition;
import com.lw.nio.action.ActionBeanFactory;
import com.lw.nio.action.DefaultRequestResponseDealer;
import com.lw.nio.action.IRequestResponseDealer;
import sun.nio.ch.Net;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author leiWei
 * 服务器端会话层
 */
public class ServerConversation {
    private static NetMessage ARE_YOU_OK;
    static {
        ARE_YOU_OK = new NetMessage().setCommond(ENetCommond.ARE_YOU_OK).setMessage(new byte[]{});
    }
    private NIOCommunication communication;
    private Server server;
    private Socket socket;
    //会话层id，可根据id得到当前id的会话
    private String id;
    private IRequestResponseDealer requestResponseDealer;


    public ServerConversation(Server server, Socket socket, ThreadPoolExecutor threadPool) throws IOException {
        this.server = server;
        this.communication = new NIOCommunication(socket);
        this.communication.setCommunication(new ServerCommunication());
        this.communication.setThreadPool(threadPool);
        this.requestResponseDealer = new DefaultRequestResponseDealer();
        this.socket = socket;
        String ip = this.socket.getInetAddress().getHostAddress();
        this.id = ip + "@" + String.valueOf(this.socket.hashCode());
    }

    String getId() {
        return id;
    }

    /**
     * 判断会话是否存活
     * @return
     */
    public boolean isAlive() {
        return this.communication.isAlive();
    }

    /**
     * 会话下线原因
     * @return
     */
    public boolean isPeerAbnormalDrop() {
        return this.communication.isPeerAbnormalDrop();
    }

    /**
     * 对于会话池中的所有会话不断轮询，调用轮询
     */
    public void check() {
        this.communication.checkMessage();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerConversation that = (ServerConversation) o;
        return Objects.equals(socket, that.socket);
    }

    @Override
    public int hashCode() {

        return Objects.hash(socket);
    }

    //TODO 与客户端的通信

    /************************服务器向客户端发送信息******************************/

    /**
     * 群发信息
     * @param id
     * @param message
     */
    void toOther(String id, String message) {
        this.communication.send(new NetMessage().setCommond(ENetCommond.TO_OTHER)
                            .setAction(id)
                            .setMessage(message));
    }

    /**
     * 私发信息
     * @param id
     * @param message
     */
    public void toOne(String id, String message) {
        NetMessage netMessage = new NetMessage().setAction(id)
                .setCommond(ENetCommond.TO_ONE)
                .setMessage(message);
        this.communication.send(netMessage);
    }

    void offline() {
        this.communication.send(new NetMessage().setCommond(ENetCommond.OFFLINE)
        .setMessage(new byte[]{}));
    }

    /**
     * 连接到服务器后，服务器向客户端发送会话专属id
     */
    void sendId() {
        NetMessage netMessage = new NetMessage().setCommond(ENetCommond.ID)
                .setAction(this.id)
                .setMessage(new byte[]{});
        this.communication.send(netMessage);
    }
    /**
     * 对于死点的标记在底层的两次send操作中完成
     */
    void areYouOk() {
        this.communication.send(ARE_YOU_OK);
    }

    /************************服务器接收客户端的信息******************************/
    void dealToOne(NetMessage netMessage) {
        String tarId = netMessage.getAction();
        String message = netMessage.getString();
        this.server.toOne(this.id, tarId, message);
    }

    void dealToOther(NetMessage netMessage) {
        String noId = netMessage.getAction();
        String message = netMessage.getString();
        this.server.toOther(this.id, noId, message);
    }

    void dealOffline(NetMessage netMessage) {
        offline();
    }

    void dealRequest(NetMessage netMessage) {
        String action = netMessage.getAction();
        String argument = netMessage.getString();
        int index = action.indexOf("*");
        String requestAction = action.substring(0, index);
        String responseAction = action.substring(index + 1);

        try {
            String strResult = this.requestResponseDealer.dealRequest(requestAction, argument);
            this.communication.send(new NetMessage().setCommond(ENetCommond.RESPONSE)
                .setAction(responseAction)
                .setMessage(strResult));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 处理消息以及客户端异常宕机的操作
     */
    class ServerCommunication implements ICommunication{

        //由App层实现
        @Override
        public void peerAbnormalDrop() {
            server.getServerAction().clientPeerAbnormalDrop();
        }

        @Override
        public void dealMessage(NetMessage message) {
            try {
                NetMessageDealer.dealMessage(ServerConversation.this, message);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}

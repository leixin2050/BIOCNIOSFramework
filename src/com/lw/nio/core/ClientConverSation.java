package com.lw.nio.core;

import com.lw.nio.action.DefaultRequestResponseDealer;
import com.lw.nio.action.IRequestResponseDealer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;

/**
 * @author leiWei
 * 客户端会话层，负责底层的通信逻辑的实现，client大哥只需要调用会话池的服务就可以完成通信操作
 */
public class ClientConverSation {
    private BIOCommunication bioCommunication;
    private Client client;
    private String id;
    private IRequestResponseDealer requestResponseDealer;
    private IClientAction clientAction;

    public ClientConverSation(Client client, Socket socket) throws IOException {
        this.client = client;
        this.clientAction = this.client.getClientAction();
        this.requestResponseDealer = new DefaultRequestResponseDealer();
        this.bioCommunication = new BIOCommunication(socket);
        this.bioCommunication.setCommunication(new CLientCommunucation());
        this.bioCommunication.startListen();
    }

    //TODO 与服务器端的通信

    String getId() {
        if (this.id == null) {
            synchronized(ClientConverSation.class) {
                if (this.id == null) {
                    //保证得到的id不为空值
                    try {
                        ClientConverSation.class.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return this.id;
    }
    /*******************************客户端对服务器发送的信息的处理***********************************/
    void dealId(NetMessage netMessage) {
        this.id = netMessage.getAction();
        synchronized (ClientConverSation.class) {
            ClientConverSation.class.notify();
        }
    }

    void dealToOne(NetMessage netMessage) {
       String ownId = netMessage.getAction();
       String mess = netMessage.getString();
       this.clientAction.toOne(ownId, mess);
    }

    void dealToOther(NetMessage netMessage) {
        String ownId = netMessage.getAction();
        String mess = netMessage.getString();
        this.clientAction.toOther(ownId, mess);
    }

    void dealOffline(NetMessage netMessage) {
        this.bioCommunication.close();
    }

    void dealResponse(NetMessage netMessage) {
        String responseAction = netMessage.getAction();
        String argument = netMessage.getString();
        try {
            this.requestResponseDealer.dealResponse(responseAction, argument);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*******************************客户端向服务器发送的信息***********************************/
    void toOne(String id, String message) {
        NetMessage netMessage = new NetMessage().setCommond(ENetCommond.TO_ONE)
                .setAction(id)
                .setMessage(message);
        this.bioCommunication.send(netMessage);
    }

    void toOther(String id, String message) {
        this.bioCommunication.send(new NetMessage().setCommond(ENetCommond.TO_OTHER)
                .setAction(id)
                .setMessage(message));
    }

    void offline() {
        this.bioCommunication.send(new NetMessage().setCommond(ENetCommond.OFFLINE)
                .setMessage(new byte[]{}));
    }

    void request(String action, String argument) {
        request(action, action, argument);
    }

    void request(String requestAction, String responseAction, String argument) {
        String action = requestAction + "*" + responseAction;
        this.bioCommunication.send(new NetMessage().setCommond(ENetCommond.REQUEST).setAction(action)
            .setMessage(argument));
    }


    //实现信息的处理与对端异常掉线
    class CLientCommunucation implements ICommunication {

        public CLientCommunucation() {
        }

        @Override
        public void peerAbnormalDrop() {
            clientAction.serverPeerAbnormalDrop();
        }

        //对于底层BIOCommunication接收到的信息的处理
        @Override
        public void dealMessage(NetMessage message) {
            try {
                NetMessageDealer.dealMessage(ClientConverSation.this, message);
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

package com.lw.nio.core;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author leiWei
 * 服务器端通信类 采用NIO通信模式
 */
public class NIOCommunication extends Communication{
    //采用NIO的通信模式，侦听线程也可以开启很多个，加快了信息效率
    private ThreadPoolExecutor threadPool;
    //alive的作用是标识此客户端是否还存活
    private volatile boolean alive;
    //busy的作用是标识当前是否真正处理消息，如果不设置的话，轮询多条消息会导致轮询到的数据紊乱
    private volatile boolean busy;
    private volatile boolean peerAbnormalDrop;


    public NIOCommunication(Socket socket) throws IOException {
        super(socket);
        this.busy = false;
        this.alive = true;
        this.peerAbnormalDrop = false;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public boolean isPeerAbnormalDrop() {
        return peerAbnormalDrop;
    }

    public void setPeerAbnormalDrop(boolean peerAbnormalDrop) {
        this.peerAbnormalDrop = peerAbnormalDrop;
    }

    //线程池需要从外部set进来
    public void setThreadPool(ThreadPoolExecutor threadPool) {
        this.threadPool = threadPool;
    }

    /**
     * 第二次检查信息发送是否成功（对端是否存活）
     * @param netMessage
     * @return
     */
    @Override
    public boolean send(NetMessage netMessage) {
        //加锁的意义在于不可同时发送两个消息，会使得字节流混乱
        synchronized(this.dos) {
            //存活时才发送信息
            if (isAlive()) {
                if (super.send(netMessage)) {
                    ENetCommond commond = netMessage.getCommond();
                    //当服务器端接收到客户端的下线信息时会同样发送一个信令为OFF_LINE的信息，供服务器内部进行对此下线客户端的操作
                    if (commond.equals(ENetCommond.OFFLINE)) {
                        this.alive = false;
                        this.peerAbnormalDrop = false;
                        close();
                    }
                } else {
                    //这里发现死点，标记死点且执行发现死点后的操作,即客户端异常宕机时服务端的操作（由后续会话层的实现ICommunication的操作）clientAbnormalDrop();
                    clientPeerAbnormalDrop();
                }
            }
        }
        return true;
    }

    /**
     * 轮询缓冲区中的信息
     */
    void checkMessage() {
        if (!this.alive) {
            return;
        }

        try {
            int len = this.dis.available();
            if (!isBusy() && len >= 0) {
                //处理一个信息的同时不可以别的线程进行receive操作，不然会导致消息接收错乱
                this.busy = true;
                //提供两种线程的执行方式 未设置进来线程池时采用创建新线程来执行
                if (threadPool == null) {
                    new Thread(new ClientPollinger()).start();
                } else {
                    this.threadPool.execute(new ClientPollinger());
                }
            }
        } catch (IOException e) {
            //客户端方出现问题，无法确定故障出现位置
            e.printStackTrace();
        }
    }



    /**
     * 对于客户端异常掉线的处理，且加锁只执行一次，设置状态以及关闭通信信道
     * 这里不可以简单的调用，因为如果简单调用的话，很有可能在同一时间收发都出现问题，导致多次调用异常宕机操作
     */
    private void clientPeerAbnormalDrop() {
        if (this.peerAbnormalDrop == false) {
            synchronized(NIOCommunication.class) {
                if (this.peerAbnormalDrop == false) {
                    setPeerAbnormalDrop(true);
                    setAlive(false);
                    close();
                    this.communication.peerAbnormalDrop();
                }
            }
        }
    }

    /**
     * 接收处理客户端发送的信息
     */
    class ClientPollinger implements Runnable{
        public ClientPollinger() {
        }

        @Override
        public void run() {
            try {
                NetMessage message = receive();
                //处理完毕后设置busy为false，可以继续接收
                busy = false;
                //接口处理接收到的信息
                communication.dealMessage(message);
            } catch (IOException e) {
                //接收出现异常，大概率是对端发生异常掉线
                clientPeerAbnormalDrop();
            }
        }
    }

}

package com.lw.nio.core;

import com.lw.nio.core.Communication;
import com.lw.nio.core.ENetCommond;
import com.lw.nio.core.NetMessage;

import java.io.IOException;
import java.net.Socket;

/**
 * @author leiWei
 * 客户端通信层，基于BIO模式，阻塞式通信
 */
public class BIOCommunication extends Communication implements Runnable{
    //lock锁，保证侦听线程的成功开启
    private Object lock;
    //控制侦听线程是否继续的开关
    private volatile boolean goon;

    public BIOCommunication(Socket socket) throws IOException {
        //完成了通信信道的建立
        super(socket);
        this.goon = false;
        this.lock = new Object();
    }

    public void startListen() {
        synchronized(lock) {
            this.goon = true;
            new Thread(this).start();

            try {
                this.lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        synchronized(lock) {
            this.lock.notify();
        }

        while (this.goon) {
            try {
                //正常接收信息
                NetMessage message = super.receive();
                ENetCommond commond = message.getCommond();
                if (commond.equals(ENetCommond.ARE_YOU_OK)) {
                    continue;
                }
                //由提供的接口处理接收到的消息，对于ARE_YOU_OK，此信息是作为检查客户端是否正常在线的，并不需要处理
                this.communication.dealMessage(message);
            } catch (IOException e) {
                if (this.goon) {
                    //服务器异常掉线,接收失败
                    this.goon = false;
                    this.communication.peerAbnormalDrop();
                }
            }
        }
    }

    @Override
    public void close() {
        super.close();
        this.goon = false;
    }
}

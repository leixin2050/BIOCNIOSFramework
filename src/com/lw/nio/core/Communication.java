package com.lw.nio.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * @author leiWei
 *  底层通信类的实现，在对NioCommunication与BioCommunication两个通信类的实现过程中
 *  发现可以提取二者的共同点作为一个基类Communication类
 */
public class Communication {
    //输入输出通信信道
    protected Socket socket;
    protected DataOutputStream dos;
    protected DataInputStream dis;

    //信息的发送与接收
    protected MessageTransfer messageTransfer;

    //外部接口
    protected ICommunication communication;

    public Communication(Socket socket) throws IOException {
        this.socket = socket;
        this.messageTransfer = new MessageTransfer();
        this.dis = new DataInputStream(this.socket.getInputStream());
        this.dos = new DataOutputStream(this.socket.getOutputStream());
    }

    //设置进来自定义处理方案
    void setCommunication(ICommunication communication) {
        this.communication = communication;
    }

    //封装底层的MessageTransfer类
    void setBufferSize(int bufferSize) {
        this.messageTransfer.setBufferSize(bufferSize);
    }

    /**
     * 发送信息
     * 且第一次判断发送是否成功（对端是否存活）
     * @param netMessage
     * @return
     */
    public boolean send(NetMessage netMessage) {
        try {
            this.messageTransfer.send(dos, netMessage);
            return true;//表示发送成功
        } catch (IOException e) {
            e.printStackTrace();
            //表示发送失败
            return false;
        }
    }

    /**
     * 接收信息
     * 且抛出异常让外部处理
     * @return
     * @throws IOException
     */
    public NetMessage receive() throws IOException {
        return this.messageTransfer.receive(dis);
    }
    
    public void close() {
        if (this.dis != null) {
            try {
                this.dis.close();
            } catch (IOException e) {
                this.dis = null;
            }
        }
        
        if (this.dos != null) {
            try {
                this.dos.close();
            } catch (IOException e) {
                this.dos = null;
            }
        }

        if (this.socket != null || !this.socket.isClosed()) {
            try {
                this.socket.close();
            } catch (IOException e) {
                this.socket = null;
            }
        }
    }

}

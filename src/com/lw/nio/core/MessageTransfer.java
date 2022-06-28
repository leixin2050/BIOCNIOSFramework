package com.lw.nio.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author leiWei
 * 信息的发送与接收类
 */
public class MessageTransfer {
    public static final int DEFAULT_BUFFER_SIZE = 1 << 15;
    private int bufferSize;

    public MessageTransfer() {
        this.bufferSize = DEFAULT_BUFFER_SIZE;
    }

    //设置默认长度
    void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    void send(DataOutputStream dos, NetMessage netMessage) throws IOException{
        //发送消息头
        dos.writeUTF(netMessage.toString());

        byte[] message = netMessage.getbyte();
        if (message == null) {
            message = new byte[]{};
        }
        /**
         * 分片传输，需要提供bufferSize，片段长度
         * 如果剩余信息长度为大于片段长度，则采用片段长度为发送的片段长度
         * 否则为剩余信息长度
         */
        int length = message.length;
        int offset = 0;
        int len = 0;
        while (length > 0) {
            len = length > this.bufferSize ? this.bufferSize : length;
            dos.write(message, offset, len);
            length -= len;
            offset += len;
        }
    }

    NetMessage receive(DataInputStream dis) throws IOException {
        //接收信息头
        String mess = dis.readUTF();
        NetMessage netMessage = new NetMessage(mess);

        int length = netMessage.getLength();
        //信息体
        byte[] message = new byte[length];
        int offset = 0;
        int len = 0;

        //接收信息体
        while (length > 0) {
            len = length > this.bufferSize ? this.bufferSize : length;
            //这样第二次给len赋值是防止网络传输过程中漏传，防止出现空的片段
            len = dis.read(message, offset, len);
            length -= len;
            offset += len;
        }
        netMessage.setMessage(message);

        return netMessage;
    }
}

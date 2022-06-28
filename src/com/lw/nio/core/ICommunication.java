package com.lw.nio.core;

/**
 * @author leiWei
 * 提供给外部的接口，用来自定义出现问题的处理方案
 */
public interface ICommunication {
    void peerAbnormalDrop();
    void dealMessage(NetMessage message);
}

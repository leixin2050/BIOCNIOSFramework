package com.lw.nio.core;

/**
 * @author leiWei
 * 留给App层进行具体逻辑的实现
 */
public interface IClientAction {
    void serverPeerAbnormalDrop();

    void toOne(String id, String message);
    void toOther(String id, String message);

    void afterConnect();
    boolean ensureOffline();
    void beforeOffline();
    void afterOffline();
}

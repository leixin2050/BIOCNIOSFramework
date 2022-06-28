package com.lw.nio.core;

/**
 * @author leiWei
 */
public class ClientActionAdapter implements IClientAction{

    @Override
    public void serverPeerAbnormalDrop() {

    }

    @Override
    public void toOne(String id, String message) {

    }

    @Override
    public void toOther(String id, String message) {

    }

    @Override
    public void afterConnect() {

    }

    @Override
    public boolean ensureOffline() {
        return true;
    }

    @Override
    public void beforeOffline() {

    }

    @Override
    public void afterOffline() {

    }

}

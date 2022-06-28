package com.lw.nio.core;

import list.IPollingAction;
import list.PollingActionAdapter;
import list.PollingList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author leiWei
 * 服务器端的客户端会话池，管理全部的会话
 *
 * 单例模式
 */
public class ServerConverSationPool {
    //存储会话池的id，为了避免直接操作会话池，且可以保证实时性
    private PollingList<String> serverConversationIDList;
    //存储会话id与它的会话池的映射关系
    private Map<String, ServerConversation> stringServerConversationPool;
    private static ServerConverSationPool me;
    private static Object lock;
    static {
        lock = new Object();
    }

    private ServerConverSationPool() {
        this.serverConversationIDList = new PollingList<>();
        //设置轮询具体行为
        this.serverConversationIDList.setPollingListAction(new PollAction());
    }

    static ServerConverSationPool getNewInstance() {
        if (me == null) {
            synchronized(lock) {
                if (me == null) {
                    me = new ServerConverSationPool();
                }
            }
        }
        return me;
    }

    static boolean hasNext() {
        return me.serverConversationIDList.hasNext();
    }

    static ServerConversation next() {
        String next = me.serverConversationIDList.next();
        return me.stringServerConversationPool.get(next);
    }

    void addServerConversation(ServerConversation serverConversation) {
        synchronized (lock) {
            String id = serverConversation.getId();
            if (!me.serverConversationIDList.contains(id)) {
                me.serverConversationIDList.add(id);
                me.stringServerConversationPool.put(id, serverConversation);
            }
        }
    }

    void removeServerConversation(ServerConversation serverConversation) {
        synchronized (lock) {
            String id = serverConversation.getId();
            if (me.serverConversationIDList.contains(id)) {
                me.serverConversationIDList.remove(id);
                me.stringServerConversationPool.remove(id);
            }
        }
    }

    //暴力重置会话池
    static void reset() {
        me = new ServerConverSationPool();
    }

    //轮询,供外部调用
    public void polling() {
        synchronized (lock) {
            me.serverConversationIDList.polling();
        }
    }

    //根据id取得会话(单发)
    ServerConversation getServerConversation(String id) {
        synchronized (lock) {
            return me.stringServerConversationPool.get(id);
        }
    }

    //取得除自身外其他所有会话（群发）
    List<ServerConversation> getOtherServerConversation(String id) {
        List<ServerConversation> serverConversationList = new ArrayList<>();
        List<String> idList = me.serverConversationIDList.getElementList();
        if (idList.contains(id)) {
            idList.remove(id);
        }

        for (String sId : idList) {
            serverConversationList.add(me.stringServerConversationPool.get(sId));
        }
        return serverConversationList;
    }

    //得到所有会话
    List<ServerConversation> getAllServerconversation() {
        return me.getOtherServerConversation(null);
    }



    class PollAction implements IPollingAction<String>{

        @Override
        public void pollingAction(String s) {
            ServerConversation serverConversation = me.stringServerConversationPool.get(s);
            if (serverConversation.isAlive()) {
                serverConversation.check();
            }
        }
    }
}

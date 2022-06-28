package com.lw.nio.core;

import jdk.nashorn.internal.ir.ReturnNode;
import time.Didadida;
import util.IListener;
import util.ISpeaker;
import util.PropertiesParser;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author leiWei
 * 供外部使用的服务器
 */
public class Server implements Runnable, ISpeaker, IListener {
    public static final int DEFAULT_SERVER_PORT = 54188;
    public static final long DEFAULT_DELAY_TIME = 3000;
    private int port;
    //控制不断侦听客户端连接
    private volatile boolean goon;
    //lock对象锁
    private volatile Object lock;
    private ServerSocket server;
    //线程池，如果未设置则使用每次创建新线程
    private ThreadPoolExecutor threadPool;
    private IServerAction serverAction;

    //供外部调用的轮询开关
    private ClientPolling clientPolling;
    //客户端连接缓冲池，为空时线程阻塞
    private ClientSocketPool clientSocketPool;

    //定时进行死点查询
    private long delayTime;
    private Didadida deathChecker;

    //观察者模式广播的对象
    private List<IListener> listenerList;

    public Server() {
        this.port = DEFAULT_SERVER_PORT;
        this.goon = false;
        this.lock = new Object();
        this.listenerList = new ArrayList<>();
        this.deathChecker = new Didadida();
    }

    /**
     * 开启服务器
     */

    public void startUp() throws Exception {
        if (isStartUp()) {
            speakOut("NIO服务器已开启！");
            return;
        }

        speakOut("开启NIO服务器！");
        this.server = new ServerSocket(this.port);
        speakOut("NIO服务器已开启！");

        this.clientSocketPool = new ClientSocketPool();
        speakOut("NIO客户端连接缓冲池准备完毕！");

        ServerConverSationPool.reset();
        speakOut("NIO客户端会话池已重置！");

        this.clientPolling = new ClientPolling();
        speakOut("NIO轮询已开启");

        //死点查询采用定时器的方式，按照给定时间向客户端发送ARE_YOU_OK信息
        this.deathChecker.setDelayTime(this.delayTime);
        //设置任务
        this.deathChecker.setTask(new DeathChecker());
        this.deathChecker.startup();
        speakOut("死点检测已开启！");

        //开启侦听客户端线程
        speakOut("NIO开启侦听客户端连接...");
        synchronized (this.lock) {
            this.goon = true;
            if (this.threadPool == null) {
                new Thread(this).start();
            } else {
                threadPool.execute(this);
            }
            try {
                //第一次阻塞，保证侦听客户端线程成功开启
                this.lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 服务器宕机
     */
    public void shutDown() {
        if (!isStartUp()) {
            speakOut("NIO服务器未开启，无需宕机！");
        }
        this.deathChecker.stop();
        speakOut("NIO死点检测已关闭！");

        speakOut("NIO轮询开始关闭...");
        this.clientPolling.close();

        speakOut("NIO客户端连接缓冲池开始关闭...");
        this.clientSocketPool.closeClientSocketPool();

        close();
        speakOut("NIO服务器已关闭！");
    }

    public void setDelayTime(long delayTime) {
        this.delayTime = delayTime;
    }

    IServerAction getServerAction() {
        return serverAction;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setThreadPool(ThreadPoolExecutor threadPool) {
        this.threadPool = threadPool;
    }

    public boolean isStartUp() {
        return this.goon;
    }

    /**
     * 监听客户端连接线程
     */
    @Override
    public void run() {
        synchronized(this.lock) {
            //侦听线程成功开启后，唤醒线程
            this.lock.notify();
        }

        while (this.goon) {
            try {
                Socket client = this.server.accept();
                //为了更好的管理服务器端的客户端，创建一个客户端连接缓冲区
                this.clientSocketPool.inClient(client);
            } catch (IOException e) {
                //侦听出现异常，结束侦听
                this.goon = false;
            }
        }
        speakOut("NIO侦听客户端连接线程结束...");
    }

    private void close() {
        this.goon = false;
        if (this.server != null || this.server.isClosed()) {
            try {
                this.server.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                this.server = null;
            }
        }
     }

    //支持propergies文件配置客户端关于服务器的ip与port,当未设置时采取默认值
    public void loadConfig(String pathName) {
        PropertiesParser.load(pathName);
        int intValue;
        long longValue;

        try {
            intValue = PropertiesParser.get("server_port", Integer.class);
            if (intValue > 0) {
                setPort(intValue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            longValue = PropertiesParser.get("delay_time", Long.class);
            if (longValue > 0) {
                setDelayTime(longValue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //观察者模式，将接收到的信息原封不动广播出去
    @Override
    public void dealMessage(String message) {
        speakOut(message);
    }

    @Override
    public void addListener(IListener listener) {
        if (!this.listenerList.contains(listener)) {
             this.listenerList.add(listener);
        }
    }

    @Override
    public void removeListener(IListener listener) {
        if (this.listenerList.contains(listener)) {
            this.listenerList.add(listener);
        }
    }

    /**
     * speakOut()方法，它实现了日志的工作。因为我们实现的是底层的框架，底层是不知道上层要做什么的，
     * 但是我们必须有一个日志功能，来提示用户现在框架的状态是什么，以便用户进行调整。
     * 如果你直接把这些内容System.out.println()，可能用户开发的时候都没有控制台，这些信息是输不出来的，
     * 或者用户并不想从控制台得知这些日志，我们做框架就必须要满足他潜在的需求。
     * @param message
     */
    @Override
    public void speakOut(String message) {
        for (IListener listener : this.listenerList) {
            listener.dealMessage(message);
        }
    }

    /***************************服务器端对客户端消息的处理*************************************/
    void toOne(String id, String tarId, String message) {
        ServerConverSationPool serverConverSationPool = ServerConverSationPool.getNewInstance();
        ServerConversation serverConversation = serverConverSationPool.getServerConversation(tarId);
        serverConversation.toOne(id, message);
    }

    void toOther(String id, String noId, String message) {
        ServerConverSationPool serverConverSationPool = ServerConverSationPool.getNewInstance();
        List<ServerConversation> otherServerConversations = serverConverSationPool.getOtherServerConversation(noId);
        for (ServerConversation serverConversation : otherServerConversations) {
            serverConversation.toOther(id, message);
        }
    }


    /**
     * 定时器的task，即死点查询
     * 1、对存活的会话二次确认是否存活
     * 2、告知服务器端会话下线状态（解决服务器无法得知用户状态）
     * 3、对已经标记的死点进行清除
     */
    class DeathChecker implements Runnable {

        public DeathChecker() {
        }

        @Override
        public void run() {
            ServerConverSationPool converSationPool = ServerConverSationPool.getNewInstance();
            while (ServerConverSationPool.hasNext()) {
                ServerConversation serverConversation = ServerConverSationPool.next();
                if (serverConversation.isAlive()) {
                    serverConversation.areYouOk();
                }
                //由于是多线程处理，这里需要再次判断存活状态
                if (!serverConversation.isAlive()) {
                    String conversationId = serverConversation.getId();
                    if (serverConversation.isPeerAbnormalDrop()) {
                        speakOut("客户端【" + conversationId + "】异常掉线！");
                    } else {
                        speakOut("客户端【" + conversationId + "】正常下线！");
                    }
                    converSationPool.removeServerConversation(serverConversation);
                }
            }
        }
    }

    /**
     * NIO客户端连接缓冲池，减轻服务器压力
     */
    class ClientSocketPool implements Runnable {
        //lcok锁，控制线程的阻塞与唤醒
        private Object lock;
        //控制线程的继续与停止
        private boolean goon;
        //使用队列存储客户端连接
        private Queue<Socket> socketQueue;

        public ClientSocketPool() {
            this.lock = new Object();
            //线程安全的队列
            this.socketQueue = new LinkedBlockingQueue<>();

            synchronized(this.lock) {
                this.goon = true;
                if (threadPool == null) {
                    new Thread(this).start();
                } else {
                    threadPool.execute(this);
                }

                try {
                    //第一次阻塞，保证线程成功开启
                    this.lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void inClient(Socket socket) {
            synchronized (this.lock) {
                this.socketQueue.offer(socket);
                //唤醒因客户端连接池为空而阻塞的线程
                this.lock.notify();
            }
        }

        @Override
        public void run() {
            synchronized(this.lock) {
                //第一次唤醒线程
                this.lock.notify();
            }


            speakOut("NIO开始处理客户端连接....");
            ServerConverSationPool serverConverSationPool = ServerConverSationPool.getNewInstance();
            while (this.goon) {
                //如果此客户端连接池为空，则阻塞此线程
                if (this.socketQueue.isEmpty()) {
                    try {
                        this.lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //运行到这里时证明连接池非空
                Socket client = this.socketQueue.poll();
                if (client == null) {
                    continue;
                }
                //TODO 处理客户端连接，即建立客户端会话池，进行通信
                try {
                    ServerConversation serverConversation = new ServerConversation(Server.this, client, threadPool);

                    serverConverSationPool.addServerConversation(serverConversation);
                    //客户端连接上服务器后,服务器向客户端发送一个携带会话id的信息
                    serverConversation.sendId();
                    speakOut("客户端【" + serverConversation.getId() + "】已连接到服务器！");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            speakOut("NIO停止处理客户端连接....");
        }

        public void closeClientSocketPool() {
            //这里再次唤醒线程，防止存在线程阻塞无法结束的情况
            synchronized (this.lock) {
                this.goon = false;
                this.lock.notify();
            }
        }
    }

    //轮询类
    class ClientPolling implements Runnable {
        private Object lock;
        private boolean goon;

        public ClientPolling() {
            this.lock = new Object();

            synchronized (this.lock) {
                this.goon = true;
                if (threadPool == null) {
                    new Thread(this).start();
                } else {
                    threadPool.execute(this);
                }

                //保证线程的正确开启
                try {
                    this.lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void run() {
            synchronized(this.lock) {
                this.lock.notify();
            }

            while (this.goon) {
                ServerConverSationPool serverConverSationPool = ServerConverSationPool.getNewInstance();
                serverConverSationPool.polling();
            }
            speakOut("NIO停止轮询客户端！");
            close();
        }

        private void close() {
            this.goon = false;
        }
    }
}

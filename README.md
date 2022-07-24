  此项目旨在实现一个服务器端采用NIO通信模式，而客户端采用BIO通信模式的工具型框架，且此框架可以二次开发，添加其他更完善的功能与操作。
该框架将完成如下几个功能：
- 服务器与客户端一对多，实现长连接，服务器端将连接上服务器的客户端会话放入一个特殊的客户端会话池中统一。
- 服务器定期对客户端连接池中的所有用户发送信息，判断用户是否掉线
- 服务器端采用轮询的方式处理缓存区中接收到的字节流
- 客户端可以向服务器发起请求，请求完毕后等待服务器的响应信息
- 提供APP接口，供进一步开发
- 客户端可以进行一对一，一对多的发送消息
- 实现分发器
- 提供通讯日志（可配置）

​	NIO是一种非阻塞的IO(网络通信)模式，它与BIO相对应，BIO采用的是阻塞式的网络通信，BIO模式最大的缺点是不能开启过多的侦听线程，开启过多的侦听线程会使得服务器的负载增大，效率急剧降低。当然BIO也有它的优势，它的灵敏度高，实时性好，尤其的客户端一旦异常下线，服务器立刻就能感知到，此外服务器的主动性非常强，可以主动给客户端发送信息（推送）。

​	而与之对应的NIO模式对于网络信息的处理方式不同，NIO模式采用一种轮询的方式，如果侦听到存在消息，不需要阻塞线程，这种非阻塞的机制使得NIO模式的服务器端可以开启很多个侦听线程，大大减轻了服务器端的负载，同样，此模式也存在劣势，它的劣势在于服务器灵敏度不强，无法感知到客户端的异常掉线，需要我们采取一些处理方式来解决此问题。

​	既然NIO与BIO模式均有他们的优势与劣势，那么我们便采用二者结合的方式搭建起一个框架，为了减轻服务器端的负载与加快效率，在服务器端采用NIO模式，而在客户端则采用灵敏度高实时性好的BIO模式。
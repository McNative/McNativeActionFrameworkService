package org.mcnative.actionframework.service.endpoint.service;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.handler.ssl.SslContext;
import net.pretronic.libraries.utility.NettyUtil;
import net.pretronic.libraries.utility.exception.OperationFailedException;
import org.mcnative.actionframework.sdk.common.protocol.MAFProtocol;
import org.mcnative.actionframework.service.endpoint.MAFEndpoint;
import org.mcnative.actionframework.service.endpoint.service.connection.ConnectionHandler;

import java.net.InetSocketAddress;

public class MAFService {

    private final MAFEndpoint endpoint;
    private final EventLoopGroup parent, child;
    private final ServerBootstrap server;
    private final SslContext sslContext;
    private final InetSocketAddress address;

    public MAFService(MAFEndpoint endpoint,InetSocketAddress address) {
        this(endpoint,address,null);
    }

    public MAFService(MAFEndpoint endpoint,InetSocketAddress address, SslContext sslContext) {
        this.endpoint = endpoint;
        this.sslContext = sslContext;
        this.address = address;

        this.parent = NettyUtil.newEventLoopGroup();
        this.child = NettyUtil.newEventLoopGroup();

        this.server = new ServerBootstrap().group(parent,child)
                .option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
                .channel(NettyUtil.getServerSocketChannelClass())
                .childOption(ChannelOption.AUTO_READ, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE,true)
                .childHandler(new MAFInitializer());
    }

    /**
     * Start this node service asynchronously.
     */
    public void startAsync(){
        new Thread(this::start,"MAFService - "+this.address.getPort()).start();
    }

    /**
     * Start this node service synchronously, we recommend to use the async way.
     *
     * <p>If you start a service sync your process will wait until the service has stopped.</p>
     */
    public void start(){
        try{
            ChannelFuture future = this.server.bind(address).addListener((ChannelFutureListener) future1 -> {
                if(future1.isSuccess()){
                    endpoint.getLogger().info("MAF service is listening on {}:{}",address.getAddress().getHostAddress(),address.getPort());
                }else{
                    endpoint.getLogger().error("MAF service could not listen on {}:{}",address.getAddress().getHostAddress(),address.getPort());
                }
            }).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            future.syncUninterruptibly().channel().closeFuture().sync();
        }catch (Exception exception){
            throw new OperationFailedException("Could not startup MFA service on"+address.toString(),exception);
        }
    }

    /**
     * Shut this node service down and break all connections immediately. We recommend to
     * use the shutdownGracefully method, the gracefully method will first disconnect
     * all connected clients and then shutdown.
     *
     */
    public void shutdown(){
        if(parent != null) parent.shutdownGracefully();
        if(child != null) child.shutdownGracefully();
    }

    /**
     * This gracefully method will first disconnect all connected clients and then shutdown.
     */
    public void shutdownGracefully(){
        shutdown();
    }

    private class MAFInitializer extends ChannelInitializer<Channel> {
        protected void initChannel(Channel channel) {
          //  if(sslContext != null) channel.pipeline().addLast(sslContext.newHandler(channel.alloc()));
            MAFProtocol.initializeChannel(channel,null,null);
            channel.pipeline().addLast(new ConnectionHandler(endpoint));
        }
    }
}

package org.mcnative.actionframework.server.service.connection;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.mcnative.actionframework.sdk.common.protocol.packet.ActionPacket;
import org.mcnative.actionframework.sdk.common.protocol.packet.Packet;
import org.mcnative.actionframework.sdk.common.protocol.packet.PacketTransport;
import org.mcnative.actionframework.sdk.common.protocol.packet.handshake.HandshakePacket;
import org.mcnative.actionframework.sdk.common.protocol.packet.handshake.HandshakeResultPacket;
import org.mcnative.actionframework.sdk.common.protocol.packet.handshake.authentication.AuthenticationResult;
import org.mcnative.actionframework.server.MAFEndpoint;

import java.io.IOException;
import java.util.UUID;

public class ConnectionHandler extends SimpleChannelInboundHandler<PacketTransport> {

    private static final byte[] EMPTY = new byte[0];

    private final MAFEndpoint endpoint;
    private ClientConnection connection;

    public ConnectionHandler(MAFEndpoint endpoint){
        this.endpoint = endpoint;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        this.connection = this.endpoint.getConnectionController().registerConnection(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.endpoint.getConnectionController().unregisterConnection(this.connection);
        if(connection.isAuthenticated()) {
            endpoint.getMessageBroker().publishAction(connection,"cln","disconnect",EMPTY);
        }
        this.connection = null;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if(!(cause instanceof IOException)) endpoint.getLogger().error("An error occurred in connection",cause);
        connection.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PacketTransport transport) {
        if(connection == null) return;
        Packet packet = transport.getPacket();
        if(connection.isAuthenticated()){
            processPacket(transport.getTransactionId(),transport.getPacket());
        }else if(packet instanceof HandshakePacket){
            AuthenticationResult result = this.endpoint.getConnectionController().authenticateConnection(this.connection,(HandshakePacket) packet);
            ctx.channel().writeAndFlush(new PacketTransport(transport.getTransactionId(),new HandshakeResultPacket(result)));
            if(result.isSuccessful()){
                endpoint.getMessageBroker().publishAction(connection,"cln","connect",EMPTY);
            }else{
                ctx.channel().close();
            }
        }
    }

    private void processPacket(UUID transactionId, Packet packet){
        if(packet instanceof ActionPacket){
            ActionPacket apacket = (ActionPacket) packet;
            endpoint.getMessageBroker().publishAction(connection,apacket.getNamespace(),apacket.getName(),apacket.getContent());
        }
    }

}

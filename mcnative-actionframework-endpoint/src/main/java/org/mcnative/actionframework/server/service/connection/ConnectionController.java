package org.mcnative.actionframework.server.service.connection;

import io.netty.channel.Channel;
import net.pretronic.libraries.logging.PretronicLogger;
import net.pretronic.libraries.utility.Validate;
import org.mcnative.actionframework.sdk.common.action.DefaultMAFActionExecutor;
import org.mcnative.actionframework.sdk.common.protocol.packet.handshake.HandshakePacket;
import org.mcnative.actionframework.sdk.common.protocol.packet.handshake.authentication.AuthenticationMethod;
import org.mcnative.actionframework.sdk.common.protocol.packet.handshake.authentication.AuthenticationResult;
import org.mcnative.actionframework.server.authentication.AuthenticationService;

import java.util.*;

public class ConnectionController {

    private final PretronicLogger logger;
    private final Map<AuthenticationMethod,AuthenticationService<?>> authenticationServices;
    private final Collection<ClientConnection> connections;
    private final Collection<ClientConnection> pendingConnections;

    public ConnectionController(PretronicLogger logger) {
        this.logger = logger;
        this.authenticationServices = new HashMap<>();
        this.pendingConnections = new ArrayList<>();
        this.connections = new ArrayList<>();
    }

    public void registerAuthenticationService(AuthenticationMethod method, AuthenticationService<?> service){
        Validate.notNull(method,service);
        this.authenticationServices.put(method,service);
    }

    public Collection<ClientConnection> getPendingConnections() {
        return pendingConnections;
    }

    public Collection<ClientConnection> getConnections() {
        return connections;
    }

    public ClientConnection registerConnection(Channel channel){
        ClientConnection connection = new ClientConnection(channel);
        this.pendingConnections.add(connection);
        return connection;
    }

    public void unregisterConnection(ClientConnection connection){
        this.pendingConnections.remove(connection);
        this.connections.remove(connection);

        if(!connection.isAuthenticated()) return;
        this.logger.info("Client from {} [{}/{}] disconnected",connection.getChannel().remoteAddress(),connection.getClientId(),connection.getClientName());
    }

    public AuthenticationResult authenticateConnection(ClientConnection connection,HandshakePacket handshake){
        AuthenticationService<?> service = authenticationServices.get(handshake.getAuthentication().getMethod());
        if(service == null) return new AuthenticationResult(false,"Unsupported authentication method");
        AuthenticationResult result = service.InvokeAuthenticate(handshake.getAuthentication());

        if(result.isSuccessful()){
            connection.setAuthResult(handshake.getUniqueId(),handshake.getName(),new DefaultMAFActionExecutor(result.getNetworkId(),handshake.getUniqueId()));
            this.connections.add(connection);
            this.pendingConnections.remove(connection);
            this.logger.info("Client from {} [{}/{}] connected",connection.getChannel().remoteAddress(),handshake.getUniqueId(),handshake.getName());
        }
        return result;
    }

    public void closeInvalidConnections(){
        for (ClientConnection connection : this.pendingConnections) {
            if(connection.getConnectTime()+3000 > System.currentTimeMillis()){
                connection.close();
            }
        }
    }

}

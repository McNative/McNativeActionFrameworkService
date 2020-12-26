package org.mcnative.actionframework.server.service.connection;

import io.netty.channel.Channel;
import org.mcnative.actionframework.sdk.common.action.MAFActionExecutor;

import java.util.UUID;

public class ClientConnection {

    private final long connectTime;
    private final Channel channel;

    private UUID clientId;
    private String clientName;

    private MAFActionExecutor actionExecutor;

    public ClientConnection(Channel channel) {
        this.channel = channel;
        this.connectTime = System.currentTimeMillis();
    }

    public long getConnectTime() {
        return connectTime;
    }

    public Channel getChannel() {
        return channel;
    }

    public UUID getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public MAFActionExecutor getActionExecutor() {
        return actionExecutor;
    }

    public boolean isAuthenticated() {
        return actionExecutor != null;
    }

    public void close(){
        this.channel.close();
    }

    public void setAuthResult(UUID clientId, String clientName,MAFActionExecutor actionExecutor) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.actionExecutor = actionExecutor;
    }
}

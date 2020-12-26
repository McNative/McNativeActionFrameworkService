package org.mcnative.actionframework.server.broker;

import org.mcnative.actionframework.server.service.connection.ClientConnection;

public interface MessageBroker {

    void publishAction(ClientConnection connection,String namespace, String name, byte[] content);

    void subscribe(ClientConnection connection);
}

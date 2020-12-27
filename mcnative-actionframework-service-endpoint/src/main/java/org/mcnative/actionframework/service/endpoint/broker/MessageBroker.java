package org.mcnative.actionframework.service.endpoint.broker;

import org.mcnative.actionframework.service.endpoint.service.connection.ClientConnection;

public interface MessageBroker {

    void publishAction(ClientConnection connection,String namespace, String name, byte[] content);

    void subscribe(ClientConnection connection);
}

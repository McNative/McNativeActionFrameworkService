package org.mcnative.actionframework.service.endpoint.broker;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import net.pretronic.libraries.utility.exception.OperationFailedException;
import net.pretronic.libraries.utility.io.IORuntimeException;
import org.mcnative.actionframework.service.endpoint.service.connection.ClientConnection;

import java.io.IOException;

public class RabbitMqBroker implements MessageBroker{

    private static final String EXCHANGE_BROADCAST = "maf-broadcast";
    private static final String EXCHANGE_DIRECT = "maf-direct";

    private final Channel channel;
    //private final String directQueue;

    public RabbitMqBroker(String host,String username,String password){
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setUsername(username);
            factory.setPassword(password);
            Connection connection = factory.newConnection();

            this.channel = connection.createChannel();
            this.channel.exchangeDeclare(EXCHANGE_BROADCAST,"topic");
            this.channel.exchangeDeclare(EXCHANGE_DIRECT,"direct");
          //  this.directQueue = this.channel.queueDeclare().getQueue();
        }catch (Exception e){
            throw new OperationFailedException(e);
        }
    }

    @Override
    public void publishAction(ClientConnection connection, String namespace, String name, byte[] content) {
        try {
            String key = connection.getActionExecutor().getNetworkIdShort()
                    +"."+connection.getActionExecutor().getClientIdShort()
                    +"."+namespace+"."+name;
            channel.basicPublish(EXCHANGE_BROADCAST,key, null,content);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    @Override
    public void subscribe(ClientConnection connection) {
        /*
        try {
           // this.channel.queueBind(this.directQueue,EXCHANGE_DIRECT,String.valueOf(null),null);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
         */
    }
}

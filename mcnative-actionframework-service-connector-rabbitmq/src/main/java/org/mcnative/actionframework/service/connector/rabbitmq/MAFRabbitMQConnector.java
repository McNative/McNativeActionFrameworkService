package org.mcnative.actionframework.service.connector.rabbitmq;

import com.rabbitmq.client.*;
import net.pretronic.libraries.utility.Iterators;
import net.pretronic.libraries.utility.StringUtil;
import net.pretronic.libraries.utility.exception.OperationFailedException;
import net.pretronic.libraries.utility.io.IORuntimeException;
import net.pretronic.libraries.utility.reflect.UnsafeInstanceCreator;
import org.mcnative.actionframework.sdk.common.action.DefaultMAFActionExecutor;
import org.mcnative.actionframework.sdk.common.action.MAFAction;
import org.mcnative.actionframework.sdk.common.action.MAFActionListener;
import org.mcnative.actionframework.sdk.common.action.MAFActionSubscription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class MAFRabbitMQConnector implements DeliverCallback {

    private static final String EXCHANGE_BROADCAST = "maf-broadcast";

    private final ConnectionFactory factory;
    private final boolean keepAlive;
    private final List<MAFActionSubscription> subscriptions;

    private Connection connection;
    private Channel channel;
    private String queue;

    private MAFRabbitMQConnector(ConnectionFactory factory,String queue, boolean keepAlive){
        this.factory = factory;
        this.keepAlive = keepAlive;
        this.subscriptions = new ArrayList<>();
        this.queue = queue;
    }

    public void connect(){
        try {
            this.connection = factory.newConnection();

            this.channel = connection.createChannel();
            this.channel.exchangeDeclare(EXCHANGE_BROADCAST,"topic");

            if(queue == null){
                this.queue = this.channel.queueDeclare().getQueue();
            }else{
                this.channel.queueDeclare(queue,true,false,!keepAlive,null);
            }
            this.channel.basicConsume(this.queue,true,this,consumerTag -> { });
        }catch (IOException | TimeoutException e){
            throw new OperationFailedException(e);
        }
    }

    public <T extends MAFAction> void subscribeAction(Class<T> actionClass, MAFActionListener<T> listener){
        MAFAction action = UnsafeInstanceCreator.newInstance(actionClass);
        try {
            channel.queueBind(this.queue,EXCHANGE_BROADCAST,"#.#."+action.getNamespace()+"."+action.getName());
            this.subscriptions.add(new MAFActionSubscription(action.getNamespace(),action.getName(),actionClass,listener));
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    public void disconnect(){
        try {
            this.channel.close();
            this.connection.close();
        }catch (IOException | TimeoutException ignored){}
    }

    @Override
    public void handle(String consumerTag, Delivery message) {
        String[] parts = StringUtil.split(message.getEnvelope().getRoutingKey(),'.');

        String network = parts[0];
        String client = parts[1];
        String namespace = parts[2];
        String name = parts[3];

        MAFActionSubscription subscription= Iterators.findOne(this.subscriptions, subscription1
                -> subscription1.getNamespace().equalsIgnoreCase(namespace) && subscription1.getName().equalsIgnoreCase(name));

        if(subscription == null) return;

        try{
            MAFAction action = UnsafeInstanceCreator.newInstance(subscription.getActionClass());
            action.readAction(message.getBody());
            subscription.getListener().callListener(new DefaultMAFActionExecutor(parseShort(network),parseShort(client)),action);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static MAFRabbitMQConnector createDedicated(ConnectionFactory factory){
        return new MAFRabbitMQConnector(factory,null,false);
    }

    public static MAFRabbitMQConnector createShared(ConnectionFactory factory,String name,boolean keepAlive){
        return new MAFRabbitMQConnector(factory,name,keepAlive);
    }

    private static UUID parseShort(String short0){
        try{
            String[] parts = StringUtil.split(short0,'/');
            return new UUID(Long.parseLong(parts[0]),Long.parseLong(parts[1]));
        }catch (Exception e){
            e.printStackTrace();
            throw e;
        }
    }

}

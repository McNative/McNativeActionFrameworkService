package org.mcnative.actionframework.server;

import net.pretronic.libraries.concurrent.TaskScheduler;
import net.pretronic.libraries.concurrent.simple.SimpleTaskScheduler;
import net.pretronic.libraries.logging.PretronicLogger;
import net.pretronic.libraries.logging.PretronicLoggerFactory;
import net.pretronic.libraries.utility.interfaces.ObjectOwner;
import org.mcnative.actionframework.server.broker.MessageBroker;
import org.mcnative.actionframework.server.broker.RabbitMqBroker;
import org.mcnative.actionframework.server.service.MAFService;
import org.mcnative.actionframework.server.service.connection.ConnectionController;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class MAFEndpoint {

    private final PretronicLogger logger;
    private final TaskScheduler scheduler;
    private final MAFService service;
    private final ConnectionController connectionController;
    private final MessageBroker messageBroker;

    public MAFEndpoint(InetSocketAddress address,MessageBroker broker){
        this(address,broker, PretronicLoggerFactory.getLogger(MAFEndpoint.class));
    }

    public MAFEndpoint(InetSocketAddress address,MessageBroker broker,PretronicLogger logger){
        this.logger = logger;
        this.scheduler = new SimpleTaskScheduler();
        this.service = new MAFService(this,address);
        this.connectionController = new ConnectionController(logger);
        this.messageBroker = broker;
    }

    public PretronicLogger getLogger() {
        return logger;
    }

    public MAFService getService() {
        return service;
    }

    public ConnectionController getConnectionController() {
        return connectionController;
    }

    public MessageBroker getMessageBroker() {
        return messageBroker;
    }

    public void start(){
        this.scheduler.createTask(ObjectOwner.SYSTEM).interval(2, TimeUnit.SECONDS).execute(this.connectionController::closeInvalidConnections);
        service.start();
    }
}

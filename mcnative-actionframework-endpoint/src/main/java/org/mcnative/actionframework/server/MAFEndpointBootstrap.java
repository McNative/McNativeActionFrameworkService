package org.mcnative.actionframework.server;

import io.sentry.Sentry;
import net.pretronic.databasequery.api.driver.config.DatabaseDriverConfig;
import net.pretronic.databasequery.sql.dialect.Dialect;
import net.pretronic.databasequery.sql.driver.config.SQLDatabaseDriverConfigBuilder;
import net.pretronic.libraries.logging.PretronicLogger;
import net.pretronic.libraries.logging.PretronicLoggerFactory;
import net.pretronic.libraries.logging.bridge.slf4j.SLF4JStaticBridge;
import net.pretronic.libraries.logging.io.LoggingPrintStream;
import org.mcnative.actionframework.sdk.common.protocol.packet.handshake.authentication.AuthenticationMethod;
import org.mcnative.actionframework.server.authentication.KeyAuthenticationService;
import org.mcnative.actionframework.server.broker.MessageBroker;
import org.mcnative.actionframework.server.broker.RabbitMqBroker;

import java.net.InetSocketAddress;

import static org.mcnative.actionframework.server.util.Environment.*;

public class MAFEndpointBootstrap {

    public static void main(String[] args) {
        boolean development = getEnv("ENVIRONMENT","development").equalsIgnoreCase("development");

        String dsn = getEnvOrNull("SENTRY_DSN");
        if(dsn != null && !development){
            Sentry.init(options -> options.setDsn(dsn));
        }

        PretronicLogger logger = PretronicLoggerFactory.getLogger(MAFEndpoint.class);
        LoggingPrintStream.hook(logger);
        SLF4JStaticBridge.trySetLogger(logger);

        try {
            String host = getEnv("HOST","localhost");
            int port = getIntEnv("PORT",9730);
            InetSocketAddress address = new InetSocketAddress(host,port);

            String database = getEnv("DATABASE_NAME");

            DatabaseDriverConfig<?> config = new SQLDatabaseDriverConfigBuilder()
                    .setAddress(InetSocketAddress.createUnresolved(getEnv("DATABASE_HOST"), getIntEnv("DATABASE_PORT",3306)))
                    .setDialect(Dialect.byName(getEnv("DATABASE_DIALECT",Dialect.MARIADB.getName())))
                    .setUsername(getEnv("DATABASE_USERNAME"))
                    .setPassword(getEnv("DATABASE_PASSWORD"))
                    .build();

            MessageBroker broker = new RabbitMqBroker(getEnv("RABBIT_HOST","localhost")
                    ,getEnv("RABBIT_USERNAME","localhost")
                    ,getEnv("RABBIT_PASSWORD","K2NtCrSiNvQ7qkzs#hSKSrhzDE24i8bswFcYrwWB"));

            MAFEndpoint endpoint = new MAFEndpoint(address,broker,logger);
            endpoint.getConnectionController().registerAuthenticationService(AuthenticationMethod.NETWORK_KEY,new KeyAuthenticationService(config,database));

            endpoint.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

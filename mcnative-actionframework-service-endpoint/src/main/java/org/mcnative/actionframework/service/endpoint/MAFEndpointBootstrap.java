package org.mcnative.actionframework.service.endpoint;

import io.sentry.Sentry;
import net.pretronic.databasequery.api.driver.config.DatabaseDriverConfig;
import net.pretronic.databasequery.sql.dialect.Dialect;
import net.pretronic.databasequery.sql.driver.config.SQLDatabaseDriverConfigBuilder;
import net.pretronic.libraries.logging.PretronicLogger;
import net.pretronic.libraries.logging.PretronicLoggerFactory;
import net.pretronic.libraries.logging.bridge.slf4j.SLF4JStaticBridge;
import net.pretronic.libraries.logging.io.LoggingPrintStream;
import org.mcnative.actionframework.sdk.common.protocol.packet.handshake.authentication.AuthenticationMethod;
import org.mcnative.actionframework.service.endpoint.authentication.KeyAuthenticationService;
import org.mcnative.actionframework.service.endpoint.broker.MessageBroker;
import org.mcnative.actionframework.service.endpoint.broker.RabbitMqBroker;
import org.mcnative.actionframework.service.endpoint.util.Environment;

import java.net.InetSocketAddress;

public class MAFEndpointBootstrap {

    public static void main(String[] args) {
        boolean development = Environment.getEnv("ENVIRONMENT","development").equalsIgnoreCase("development");

        String dsn = Environment.getEnvOrNull("SENTRY_DSN");
        if(dsn != null && !development){
            Sentry.init(options -> options.setDsn(dsn));
        }

        PretronicLogger logger = PretronicLoggerFactory.getLogger(MAFEndpoint.class);
        LoggingPrintStream.hook(logger);
        SLF4JStaticBridge.trySetLogger(logger);

        try {
            String host = Environment.getEnv("HOST","localhost");
            int port = Environment.getIntEnv("PORT",9730);
            InetSocketAddress address = new InetSocketAddress(host,port);

            String database = Environment.getEnv("DATABASE_NAME");

            DatabaseDriverConfig<?> config = new SQLDatabaseDriverConfigBuilder()
                    .setAddress(InetSocketAddress.createUnresolved(Environment.getEnv("DATABASE_HOST"), Environment.getIntEnv("DATABASE_PORT",3306)))
                    .setDialect(Dialect.byName(Environment.getEnv("DATABASE_DIALECT",Dialect.MARIADB.getName())))
                    .setUsername(Environment.getEnv("DATABASE_USERNAME"))
                    .setPassword(Environment.getEnv("DATABASE_PASSWORD"))
                    .build();

            MessageBroker broker = new RabbitMqBroker(Environment.getEnv("RABBIT_HOST","localhost")
                    , Environment.getEnv("RABBIT_USERNAME","localhost")
                    , Environment.getEnv("RABBIT_PASSWORD","K2NtCrSiNvQ7qkzs#hSKSrhzDE24i8bswFcYrwWB"));

            MAFEndpoint endpoint = new MAFEndpoint(address,broker,logger);
            endpoint.getConnectionController().registerAuthenticationService(AuthenticationMethod.NETWORK_KEY,new KeyAuthenticationService(config,database));

            endpoint.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

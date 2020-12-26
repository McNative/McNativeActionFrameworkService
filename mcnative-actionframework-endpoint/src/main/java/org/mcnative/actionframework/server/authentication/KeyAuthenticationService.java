package org.mcnative.actionframework.server.authentication;

import net.pretronic.databasequery.api.Database;
import net.pretronic.databasequery.api.collection.DatabaseCollection;
import net.pretronic.databasequery.api.driver.DatabaseDriver;
import net.pretronic.databasequery.api.driver.DatabaseDriverFactory;
import net.pretronic.databasequery.api.driver.config.DatabaseDriverConfig;
import net.pretronic.databasequery.api.query.result.QueryResult;
import net.pretronic.databasequery.api.query.result.QueryResultEntry;
import org.mcnative.actionframework.sdk.common.protocol.packet.handshake.authentication.AuthenticationResult;
import org.mcnative.actionframework.sdk.common.protocol.packet.handshake.authentication.KeyAuthentication;

public class KeyAuthenticationService implements AuthenticationService<KeyAuthentication> {

    private final DatabaseCollection collection;

    public KeyAuthenticationService(DatabaseDriverConfig<?> config,String databaseName){
        DatabaseDriver driver = DatabaseDriverFactory.create("KeyAuthenticationService",config);
        driver.connect();
        Database database = driver.getDatabase(databaseName);
        this.collection = database.getCollection("mcnative_network");
    }

    @Override
    public AuthenticationResult authenticate(KeyAuthentication authentication) {
        QueryResult result = collection.find().where("Id",authentication.getNetworkId().toString()).limit(1).execute();
        if(!result.isEmpty()){
            QueryResultEntry entry = result.first();
            String secret = entry.getString("Secret");
            if(authentication.getSecret().equals(secret)){
                return new AuthenticationResult(true,"Welcome to the McNative Action Framework",authentication.getNetworkId());
            }
        }
        return new AuthenticationResult(false,"Invalid network id or secret");
    }

}

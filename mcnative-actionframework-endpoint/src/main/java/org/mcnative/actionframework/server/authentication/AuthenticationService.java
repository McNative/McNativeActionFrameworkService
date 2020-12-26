package org.mcnative.actionframework.server.authentication;

import org.mcnative.actionframework.sdk.common.protocol.packet.handshake.authentication.Authentication;
import org.mcnative.actionframework.sdk.common.protocol.packet.handshake.authentication.AuthenticationResult;

public interface AuthenticationService<T extends Authentication> {

    AuthenticationResult authenticate(T authentication);

    @SuppressWarnings("unchecked")
    default AuthenticationResult InvokeAuthenticate(Authentication authentication){
        return authenticate((T) authentication);
    }

}

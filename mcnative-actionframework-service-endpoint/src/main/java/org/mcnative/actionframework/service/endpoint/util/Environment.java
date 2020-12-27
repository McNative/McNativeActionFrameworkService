package org.mcnative.actionframework.service.endpoint.util;

import io.github.cdimascio.dotenv.Dotenv;

public class Environment {

    public static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();

    public static String getEnv(String name) {
        String value = getEnvOrNull(name);
        if(value == null) throw new IllegalArgumentException("Can't load environment variable " + name);
        return value;
    }

    public static String getEnv(String name,String default0) {
        String value = getEnvOrNull(name);
        if(value == null) return default0;
        return value;
    }

    public static String getEnvOrNull(String name) {
        if (System.getenv(name) != null) {
            return System.getenv(name);
        } else if (DOTENV.get(name) != null) {
            return DOTENV.get(name);
        }
        return null;
    }

    public static int getIntEnv(String name,int default0) {
        String value = getEnvOrNull(name);
        if(value == null) return default0;
        return Integer.parseInt(value);
    }

}

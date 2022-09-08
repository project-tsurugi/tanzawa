package com.tsurugidb.console.core;

import java.net.URI;

import com.tsurugidb.tsubakuro.channel.common.connection.Credential;

/**
 * SQL scripts configuration.
 */
public class ScriptConfig {

    private URI endpoint;

    private Credential credential;

    public void setEndpoint(URI endpoint) {
        this.endpoint = endpoint;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    public Credential getCredential() {
        return credential;
    }
}

package com.redhat.labs.lodestar.rest.client;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class GitlabTokenFactory implements ClientHeadersFactory {
    public static final String GITLAB_PERSONAL_ACCESS_TOKEN = "GITLAB_PERSONAL_ACCESS_TOKEN";
    public static final Logger LOGGER = LoggerFactory.getLogger(GitlabTokenFactory.class);
    
    @ConfigProperty(name = "gitlab.personal.access.token")
    String gitLabToken;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {
        
        MultivaluedMap<String, String> result = new MultivaluedMapImpl<>();
        result.add("Private-Token", gitLabToken);

        return result;
    }
}
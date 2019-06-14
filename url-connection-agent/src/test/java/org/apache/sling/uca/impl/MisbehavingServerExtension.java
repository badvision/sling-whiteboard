/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.uca.impl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides an Jetty-based local server that can be configured to timeout
 * 
 * <p>After extending a JUnit Jupiter test with this extension, any parameter of type {@link MisbehavingServerControl}
 * will be resolved.</p>
 *
 */
class MisbehavingServerExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver, MisbehavingServerControl {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    public int getLocalPort() {
        return ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }
    
    private Server server;
    
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == MisbehavingServerControl.class;
    }
    
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if ( parameterContext.getParameter().getType() == MisbehavingServerControl.class )
            return this;
        
        throw new ParameterResolutionException("Unable to get a " + MisbehavingServerControl.class.getSimpleName() + " instance for " + parameterContext);
    }
    
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        
        server = new Server();
        ServerConnector connector = new ServerConnector(server) {
            @Override
            public void accept(int acceptorID) throws IOException {
                LOG.info("Waiting before accepting");
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(10));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                super.accept(acceptorID);
                LOG.info("Accepted");
            }
        };
        server.setConnectors(new Connector[] { connector });
        server.setHandler(new AbstractHandler() {
            
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                if ( baseRequest.getHeader("User-Agent") != null )
                    response.addHeader("Original-User-Agent", baseRequest.getHeader("User-Agent"));
                baseRequest.setHandled(true);
            }
        });
        
        server.start();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if ( server == null )
            return;
        try {
            server.stop();
        } catch (Exception e) {
            logger.info("Failed shutting down server", e);
        }
    }
}
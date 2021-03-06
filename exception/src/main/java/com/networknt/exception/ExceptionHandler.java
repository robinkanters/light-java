/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.exception;

import com.networknt.config.Config;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.status.Status;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by steve on 29/09/16.
 */
public class ExceptionHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);

    public static final String CONFIG_NAME = "exception";
    static ExceptionConfig config;
    static {
        config = (ExceptionConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ExceptionConfig.class);
    }

    static final String STATUS_RUNTIME_EXCEPTION = "ERR10010";
    static final String STATUS_UNCAUGHT_EXCEPTION = "ERR10011";

    private volatile HttpHandler next;

    public ExceptionHandler() {

    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        try {
            next.handleRequest(exchange);
        } catch (Throwable e) {
            logger.error("Exception:", e);
            if(exchange.isResponseChannelAvailable()) {
                //handle exceptions
                if(e instanceof RuntimeException) {
                    Status status = new Status(STATUS_RUNTIME_EXCEPTION);
                    exchange.setStatusCode(status.getStatusCode());
                    exchange.getResponseSender().send(status.toString());
                } else {
                    if(e instanceof ApiException) {
                        ApiException ae = (ApiException)e;
                        exchange.setStatusCode(ae.getStatus().getStatusCode());
                        exchange.getResponseSender().send(ae.getStatus().toString());
                    } else {
                        Status status = new Status(STATUS_UNCAUGHT_EXCEPTION);
                        exchange.setStatusCode(status.getStatusCode());
                        exchange.getResponseSender().send(status.toString());
                    }
                }
            }
        }
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(final HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(ExceptionHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }

}

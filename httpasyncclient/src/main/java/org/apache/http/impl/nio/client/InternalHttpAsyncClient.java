/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.apache.http.impl.nio.client;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthState;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.BasicFuture;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.Lookup;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.nio.NHttpClientEventHandler;
import org.apache.http.nio.conn.NHttpClientConnectionManager;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
// 一个client只会产生一个
class InternalHttpAsyncClient extends CloseableHttpAsyncClientBase {

    private final Log log = LogFactory.getLog(getClass());

    private final NHttpClientConnectionManager connmgr;
    private final ConnectionReuseStrategy connReuseStrategy;
    private final ConnectionKeepAliveStrategy keepaliveStrategy;
    private final InternalClientExec exec; //MainClientExec 
    private final Lookup<CookieSpecProvider> cookieSpecRegistry;
    private final Lookup<AuthSchemeProvider> authSchemeRegistry;
    private final CookieStore cookieStore; // 共享的属性
    private final CredentialsProvider credentialsProvider;
    private final RequestConfig defaultConfig;

    public InternalHttpAsyncClient(
            final NHttpClientConnectionManager connmgr,
            final ConnectionReuseStrategy connReuseStrategy,
            final ConnectionKeepAliveStrategy keepaliveStrategy,
            final ThreadFactory threadFactory,
            final NHttpClientEventHandler handler,
            final InternalClientExec exec,
            final Lookup<CookieSpecProvider> cookieSpecRegistry,
            final Lookup<AuthSchemeProvider> authSchemeRegistry,
            final CookieStore cookieStore,
            final CredentialsProvider credentialsProvider,
            final RequestConfig defaultConfig) {
        super(connmgr, threadFactory, handler);
        this.connmgr = connmgr;
        this.connReuseStrategy = connReuseStrategy;
        this.keepaliveStrategy = keepaliveStrategy;
        this.exec = exec;
        this.cookieSpecRegistry = cookieSpecRegistry;
        this.authSchemeRegistry = authSchemeRegistry;
        this.cookieStore = cookieStore;
        this.credentialsProvider = credentialsProvider;
        this.defaultConfig = defaultConfig;
    }
    // 所有管道共享的属性
    private void setupContext(final HttpClientContext context) {
        if (context.getAttribute(HttpClientContext.TARGET_AUTH_STATE) == null) { // http.auth.target-scope
            context.setAttribute(HttpClientContext.TARGET_AUTH_STATE, new AuthState());
        }
        if (context.getAttribute(HttpClientContext.PROXY_AUTH_STATE) == null) { // http.auth.proxy-scope
            context.setAttribute(HttpClientContext.PROXY_AUTH_STATE, new AuthState());
        }
        if (context.getAttribute(HttpClientContext.AUTHSCHEME_REGISTRY) == null) { // http.authscheme-registry
            context.setAttribute(HttpClientContext.AUTHSCHEME_REGISTRY, this.authSchemeRegistry);
        }
        if (context.getAttribute(HttpClientContext.COOKIESPEC_REGISTRY) == null) { // http.cookiespec-registry
            context.setAttribute(HttpClientContext.COOKIESPEC_REGISTRY, this.cookieSpecRegistry);
        }
        if (context.getAttribute(HttpClientContext.COOKIE_STORE) == null) { // http.cookie-store
            context.setAttribute(HttpClientContext.COOKIE_STORE, this.cookieStore);
        }
        if (context.getAttribute(HttpClientContext.CREDS_PROVIDER) == null) { // http.auth.credentials-provider
            context.setAttribute(HttpClientContext.CREDS_PROVIDER, this.credentialsProvider);
        }
        if (context.getAttribute(HttpClientContext.REQUEST_CONFIG) == null) { // http.request-config
            context.setAttribute(HttpClientContext.REQUEST_CONFIG, this.defaultConfig);
        }
    }
    // http发请求，就会跑到这里
    @Override
    public <T> Future<T> execute(
            final HttpAsyncRequestProducer requestProducer,
            final HttpAsyncResponseConsumer<T> responseConsumer,
            final HttpContext context,
            final FutureCallback<T> callback) {
        ensureRunning();
        final BasicFuture<T> future = new BasicFuture<T>(callback);//在释放完管道后，DefaultClientExchangeHandlerImpl.responseCompleted()会置位成功
        final HttpClientContext localcontext = HttpClientContext.adapt( // HttpClientContext,比较重要，和state搭配使用
            context != null ? context : new BasicHttpContext());
        setupContext(localcontext);

        @SuppressWarnings("resource")  // 保存着Request，在HttpAsyncRequestExecutor.requestReady()中会使用Request
        final DefaultClientExchangeHandlerImpl<T> handler = new DefaultClientExchangeHandlerImpl<T>(
            this.log,
            requestProducer,// 包含的有请求体内容
            responseConsumer,
            localcontext,
            future,
            this.connmgr,
            this.connReuseStrategy,
            this.keepaliveStrategy,
            this.exec);
        try {
            handler.start();
        } catch (final Exception ex) {
            handler.failed(ex);
        }
        return new FutureWrapper<T>(future, handler);
    }

    @Override
    public <T> Future<List<T>> execute(
            final HttpHost target,
            final List<? extends HttpAsyncRequestProducer> requestProducers,
            final List<? extends HttpAsyncResponseConsumer<T>> responseConsumers,
            final HttpContext context,
            final FutureCallback<List<T>> callback) {
        throw new UnsupportedOperationException("Pipelining not supported");
    }

}

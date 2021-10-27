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

import java.nio.ByteBuffer;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
// 每次查询都会产生一个，包含请求原始体，放在DefaultClientExchangeHandlerImpl里面
class InternalState {

    private final long id;
    private final HttpAsyncRequestProducer requestProducer;// // 最原始的url+请求体
    private final HttpAsyncResponseConsumer<?> responseConsumer;// 从ContentDecoder中读取解析后的字节流后，放入responseConsumer中
    private final HttpClientContext localContext;

    private HttpRequestWrapper mainRequest;//真正的原始请求
    private HttpResponse finalResponse;
    private ByteBuffer tmpbuf;
    private boolean requestContentProduced; // 发送前的content是否已经编码为字节流
    private int execCount;

    private int redirectCount;
    private HttpUriRequest redirect;

    public InternalState(
            final long id,
            final HttpAsyncRequestProducer requestProducer,
            final HttpAsyncResponseConsumer<?> responseConsumer,
            final HttpClientContext localContext) {
        super();
        this.id = id;
        this.requestProducer = requestProducer;
        this.responseConsumer = responseConsumer;
        this.localContext = localContext;
    }

    public long getId() {
        return id;
    }

    public HttpAsyncRequestProducer getRequestProducer() {
        return requestProducer;// // 最原始的url+请求体
    }

    public HttpAsyncResponseConsumer<?> getResponseConsumer() {
        return responseConsumer;
    }

    public HttpClientContext getLocalContext() {
        return localContext;
    }

    public HttpRequestWrapper getMainRequest() {
        return mainRequest;
    }

    public void setMainRequest(final HttpRequestWrapper mainRequest) {
        this.mainRequest = mainRequest;
    }

    public HttpResponse getFinalResponse() {
        return finalResponse;
    }

    public void setFinalResponse(final HttpResponse finalResponse) {
        this.finalResponse = finalResponse;
    }

    public ByteBuffer getTmpbuf() {
        if (tmpbuf == null) {
            tmpbuf = ByteBuffer.allocate(4 * 1024);
        }
        return tmpbuf;
    }

    public boolean isRequestContentProduced() {
        return requestContentProduced;
    }

    public void setRequestContentProduced() {
        this.requestContentProduced = true;
    }

    public int getExecCount() {
        return execCount;
    }

    public void incrementExecCount() {
        this.execCount++;
    }

    public int getRedirectCount() {
        return redirectCount;
    }

    public void incrementRedirectCount() {
        this.redirectCount++;
    }

    public HttpUriRequest getRedirect() {
        return redirect;
    }

    public void setRedirect(final HttpUriRequest redirect) {
        this.redirect = redirect;
    }

    @Override
    public String toString() {
        return Long.toString(id);
    }

}

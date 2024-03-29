/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.opendatakit.httpclientandroidlib.HttpEntity;
import org.opendatakit.httpclientandroidlib.HttpEntityEnclosingRequest;
import org.opendatakit.httpclientandroidlib.HttpResponse;
import org.opendatakit.httpclientandroidlib.client.HttpClient;
import org.opendatakit.httpclientandroidlib.client.methods.HttpUriRequest;
import org.opendatakit.httpclientandroidlib.entity.ByteArrayEntity;
import org.opendatakit.httpclientandroidlib.protocol.HTTP;
import org.opendatakit.httpclientandroidlib.protocol.HttpContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * {@link org.springframework.http.client.ClientHttpRequest} implementation that uses Apache HttpClient 4.0 to execute
 * requests.
 *
 * <p>
 * Created via the {@link HttpClientAndroidlibRequestFactory}.
 *
 * @author Oleg Kalnichevski
 * @author Roy Clarkson
 * @since 1.0
 * @see HttpClientAndroidlibRequestFactory#createRequest(java.net.URI, HttpMethod)
 */
final class HttpClientAndroidlibHttpRequest extends AbstractBufferingClientHttpRequest {

	private final HttpClient httpClient;

	private final HttpUriRequest httpRequest;

	private final HttpContext httpContext;

	public HttpClientAndroidlibHttpRequest(HttpClient httpClient, HttpUriRequest httpRequest, HttpContext httpContext) {
		this.httpClient = httpClient;
		this.httpRequest = httpRequest;
		this.httpContext = httpContext;
	}

	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.httpRequest.getMethod());
	}

	public URI getURI() {
		return this.httpRequest.getURI();
	}

	@Override
	public ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			String headerName = entry.getKey();
			if (!headerName.equalsIgnoreCase(HTTP.CONTENT_LEN) && !headerName.equalsIgnoreCase(HTTP.TRANSFER_ENCODING)) {
				for (String headerValue : entry.getValue()) {
					this.httpRequest.addHeader(headerName, headerValue);
				}
			}
		}
		if (this.httpRequest instanceof HttpEntityEnclosingRequest) {
			HttpEntityEnclosingRequest entityEnclosingReq = (HttpEntityEnclosingRequest) this.httpRequest;
			HttpEntity requestEntity = new ByteArrayEntity(bufferedOutput);
			entityEnclosingReq.setEntity(requestEntity);
		}
		HttpResponse httpResponse = httpClient.execute(this.httpRequest, this.httpContext);
		return new HttpClientAndroidlibHttpResponse(httpResponse);
	}

}

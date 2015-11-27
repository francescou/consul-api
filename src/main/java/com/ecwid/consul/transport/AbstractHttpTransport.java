package com.ecwid.consul.transport;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

public class AbstractHttpTransport implements HttpTransport {

	protected final HttpClient httpClient;

	public AbstractHttpTransport() {
		ClientConnectionManager connectionManager = new SingleClientConnManager();

		this.httpClient = new DefaultHttpClient(connectionManager);
	}

	public AbstractHttpTransport(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	@Override
	public RawResponse makeGetRequest(String url) {
		HttpGet httpGet = new HttpGet(url);
		return executeRequest(httpGet);
	}

	@Override
	public RawResponse makePutRequest(String url, String content) {
		HttpPut httpPut = new HttpPut(url);
		try {
			httpPut.setEntity(new StringEntity(content, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return executeRequest(httpPut);
	}

	@Override
	public RawResponse makePutRequest(String url, byte[] content) {
		HttpPut httpPut = new HttpPut(url);
		httpPut.setEntity(new ByteArrayEntity(content));
		return executeRequest(httpPut);
	}

	@Override
	public RawResponse makeDeleteRequest(String url) {
		HttpDelete httpDelete = new HttpDelete(url);
		return executeRequest(httpDelete);
	}

	private RawResponse executeRequest(HttpUriRequest httpRequest) {
		try {
			return httpClient.execute(httpRequest, new ResponseHandler<RawResponse>() {
				@Override
				public RawResponse handleResponse(HttpResponse response) throws IOException {
					int statusCode = response.getStatusLine().getStatusCode();
					String statusMessage = response.getStatusLine().getReasonPhrase();

					String content = EntityUtils.toString(response.getEntity(), HTTP.UTF_8);

					Long consulIndex = parseLong(response.getFirstHeader("X-Consul-Index"));
					Boolean consulKnownLeader = parseBoolean(response.getFirstHeader("X-Consul-Knownleader"));
					Long consulLastContact = parseLong(response.getFirstHeader("X-Consul-Lastcontact"));

					return new RawResponse(statusCode, statusMessage, content, consulIndex, consulKnownLeader, consulLastContact);
				}
			});
		} catch (IOException e) {
			throw new TransportException(e);
		}
	}

	private Long parseLong(Header header) {
		try {
			return Long.parseLong(header.getValue());
		} catch (Exception e) {
			return null;
		}
	}

	private Boolean parseBoolean(Header header) {
		if (header == null) {
			return null;
		}

		if ("true".equals(header.getValue())) {
			return true;
		}

		if ("false".equals(header.getValue())) {
			return false;
		}

		return null;
	}

}

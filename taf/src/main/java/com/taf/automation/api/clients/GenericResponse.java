package com.taf.automation.api.clients;

import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import com.taf.automation.api.rest.GenericBaseError;
import com.taf.automation.api.rest.GenericHttpResponse;
import com.taf.automation.api.rest.TextError;
import com.taf.automation.api.ApiUtils;

/**
 * Generic Response
 *
 * @param <T>
 */
public class GenericResponse<T> implements GenericHttpResponse<T> {
    private StatusLine status;
    private String entityAsString;
    private T entity;
    private Header[] headers;
    private GenericBaseError apiError;

    /**
     * Constructor for Generic Response
     *
     * @param response       - Response
     * @param responseEntity - Response Entity
     */
    @SuppressWarnings("unchecked")
    public GenericResponse(CloseableHttpResponse response, Class<T> responseEntity) {
        status = response.getStatusLine();
        headers = response.getAllHeaders();

        if (response.getEntity() == null) {
            return;
        }

        try {
            entityAsString = EntityUtils.toString(response.getEntity());
            String attachName = "RESPONSE ENTITY";
            if (responseEntity != null) {
                if (status.getStatusCode() < 400) {
                    entity = (T) entityAsString;
                } else {
                    attachName = "RESPONSE ERROR";
                    apiError = new TextError(entityAsString);
                }
            }

            ApiUtils.attachDataText(entityAsString, attachName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public StatusLine getStatus() {
        return status;
    }

    @Override
    public T getEntity() {
        return entity;
    }

    @Override
    public String getEntityAsString() {
        return entityAsString;
    }

    @Override
    public Header[] getAllHeaders() {
        return headers;
    }

    public GenericBaseError getErrorEntity() {
        return apiError;
    }

    public Header getHeader(String name) {
        for (Header header : headers) {
            if (header.getName().equals(name)) {
                return header;
            }
        }

        return null;
    }

}

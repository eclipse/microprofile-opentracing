/*
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.microprofile.opentracing.tck.application;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * Test web services JAXRS application.
 */
@ApplicationPath(TestWebServicesApplication.TEST_WEB_SERVICES_CONTEXT_ROOT)
public class TestWebServicesApplication extends Application {

    /**
     * The context root of JAXRS web services.
     */
    public static final String TEST_WEB_SERVICES_CONTEXT_ROOT = "rest";
    
    /**
     * An example text/plain response.
     */
    public static final String EXAMPLE_RESPONSE_TEXT = "Hello World";

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<>(Arrays.asList(
            TracerWebService.class,
            TestServerWebServices.class,
            JacksonJsonProvider.class));
    }

    /**
     * Create web service URL.
     * @param baseUri Base URI.
     * @param service Web service path
     * @param relativePath Web service endpoint
     * @return Web service URL
     */
    public static String getWebServiceURL(final URL baseUri,
            final String service, final String relativePath) {
        try {
            return new URL(baseUri,
                    TEST_WEB_SERVICES_CONTEXT_ROOT + "/"
                            + service + "/" + relativePath).toString();
        }
        catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Invoke an sync JAXRS request.
     * @param requestUrl Request URL.
     * @return HTTP response.
     */
    public static Response invoke(String requestUrl) {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(requestUrl);
        return target.request().get();
    }

    /**
     * Convert a map into a query string.
     * @param queryParameters Map to convert.
     * @return Query string.
     */
    public static String getQueryString(Map<String, Object> queryParameters) {
        String result = "?";
    
        String prefix = null;
        for (Map.Entry<String, Object> parmEntry : queryParameters
                .entrySet()) {
            String parmName = parmEntry.getKey();
            Object parmValue = parmEntry.getValue();
    
            if (prefix != null) {
                result += prefix;
            }
            else {
                prefix = "&";
            }
    
            result += urlEncode(parmName);
    
            if (parmValue != null) {
                result += "=";
                result += urlEncode(parmValue.toString());
            }
        }
        
        return result;
    }

    /**
     * URL-encode {@code text}.
     * @param text Text to encode.
     * @return Encoded text.
     */
    public static String urlEncode(String text) {
        try {
            return URLEncoder.encode(text, StandardCharsets.UTF_8.name());
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}

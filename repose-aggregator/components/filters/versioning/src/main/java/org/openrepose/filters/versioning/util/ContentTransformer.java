/*
 * #%L
 * Repose
 * %%
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * %%
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
 * #L%
 */
package org.openrepose.filters.versioning.util;

import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.transform.StreamTransform;
import org.openrepose.commons.utils.transform.Transform;
import org.openrepose.commons.utils.transform.jaxb.JaxbEntityToXml;
import org.openrepose.commons.utils.transform.jaxb.JaxbToStreamTransform;
import org.openrepose.commons.utils.transform.xslt.JaxbXsltToStringTransform;
import org.openrepose.commons.utils.transform.xslt.XsltToStreamTransform;
import org.openrepose.commons.utils.xslt.LogErrorListener;
import org.openrepose.commons.utils.xslt.LogTemplatesWrapper;
import org.openrepose.core.servlet.PowerApiContextException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.OutputStream;

public class ContentTransformer {
    private static final String JSON_XSLT = "/META-INF/transform/xslt/version-json.xsl";

    @Deprecated
    private final Transform<JAXBElement, String> jsonTransform;
    @Deprecated
    private final Transform<JAXBElement, String> xmlTransform;
    
    private final StreamTransform<JAXBElement, OutputStream> jsonStreamTransform;
    private final StreamTransform<JAXBElement, OutputStream> xmlStreamTransform;

    /**
     * This is the class that was used in the original repose before having to set the classloader
     * I don't know if this is the right class, but it does work.
     */
    private static final TransformerFactory XSLT_TRANSFORMER_FACTORY =
            TransformerFactory.newInstance("org.apache.xalan.processor.TransformerFactoryImpl", ContentTransformer.class.getClassLoader());

    static {
        XSLT_TRANSFORMER_FACTORY.setErrorListener(new LogErrorListener());
    }

    private static Templates parseXslt(Source s) throws TransformerConfigurationException {
        synchronized (XSLT_TRANSFORMER_FACTORY) {
            return new LogTemplatesWrapper(XSLT_TRANSFORMER_FACTORY.newTemplates(s));
        }
    }

    public ContentTransformer() {
        try {
            final JAXBContext context = JAXBContext.newInstance(
                    org.openrepose.filters.versioning.schema.ObjectFactory.class,
                    org.openrepose.filters.versioning.config.ObjectFactory.class);
            final Templates jsonXsltTemplates =
                    ContentTransformer.parseXslt(new StreamSource(getClass().getResourceAsStream(JSON_XSLT)));

            xmlTransform = new JaxbEntityToXml(context);
            jsonTransform = new JaxbXsltToStringTransform(jsonXsltTemplates, context);
            
            xmlStreamTransform = new JaxbToStreamTransform<OutputStream>(context);
            jsonStreamTransform = new XsltToStreamTransform<OutputStream>(jsonXsltTemplates, context);
        } catch (Exception ex) {
            throw new PowerApiContextException(
                    "Failed to build transformation processors for response marshalling. Reason: "
                    + ex.getMessage(), ex);
        }
    }

    public void transform(JAXBElement element, MediaType mediaRange, OutputStream outputStream) {
        switch (mediaRange.getMimeType()) {
            case APPLICATION_XML:
                xmlStreamTransform.transform(element, outputStream);
                break;
            case APPLICATION_JSON:
            case UNKNOWN:
            default:
                jsonStreamTransform.transform(element, outputStream);
                break;
        }
    }

    @Deprecated
    public String transform(JAXBElement element, MediaType mediaRange) {
        switch (mediaRange.getMimeType()) {
            case APPLICATION_XML:
                return xmlTransform.transform(element);

            case APPLICATION_JSON:
            case UNKNOWN:
            default:
                return jsonTransform.transform(element);
        }
    }
}

/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
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
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.rms;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.core.services.rms.config.ResponseMessagingConfiguration;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;

@RunWith(Enclosed.class)
public class SchemaTest {

    public static class TestResponseMessagingConfig {

        private Validator validator;
        private Unmarshaller jaxbUnmarshaller;
        private DocumentBuilderFactory dbf;


        @Before
        public void standUp() throws Exception {

            dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);

            SchemaFactory sf = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
            sf.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);

            Schema schema = sf.newSchema(
                    new StreamSource[]{
                            new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/response-messaging/response-messaging.xsd")),
                    });

            validator = schema.newValidator();

            jaxbUnmarshaller = JAXBContext.newInstance(
                    ResponseMessagingConfiguration.class.getPackage().getName()).createUnmarshaller();


        }


        @Test
        public void shouldValidate() throws IOException, SAXException {
            validator.validate(new StreamSource(getClass().getResourceAsStream("/META-INF/service/rms/response-messaging.cfg.xml")));
        }

        @Test
        public void shouldUnmarshall() throws SAXException, JAXBException, ParserConfigurationException, IOException {
            Document doc = dbf.newDocumentBuilder().parse(new InputSource(getClass().getResourceAsStream("/META-INF/service/rms/response-messaging.cfg.xml")));

            validator.validate(new DOMSource(doc));

            ResponseMessagingConfiguration config = jaxbUnmarshaller.unmarshal(doc, ResponseMessagingConfiguration.class).getValue();

            assertNotNull("Expected element should not be null",
                    config.getStatusCode());
        }


        @Test(expected = SAXParseException.class)
        public void shouldNotValidateDueToAssert() throws IOException, SAXException {
            validator.validate(new StreamSource(getClass().getResourceAsStream("/META-INF/service/rms/response-messaging-assert.cfg.xml")));
        }


        @Test(expected = SAXParseException.class)
        public void shouldNotUnmarshallDueToAssert() throws SAXException, JAXBException, ParserConfigurationException, IOException {
            DocumentBuilder db = dbf.newDocumentBuilder();

            Document doc = db.parse(new InputSource(getClass().getResourceAsStream("/META-INF/service/rms/response-messaging-assert.cfg.xml")));

            validator.validate(new DOMSource(doc));

            ResponseMessagingConfiguration config = jaxbUnmarshaller.unmarshal(doc.getDocumentElement(), ResponseMessagingConfiguration.class).getValue();

        }
    }
}
/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apigee.openapi.converter.parser;

import com.apigee.openapi.converter.model.ApigeeBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;

/**
 * Parser for the main Apigee API Proxy descriptor XML file.
 * This is the root XML file in the apiproxy directory (e.g., myproxy.xml).
 */
public class ProxyDescriptorParser {

    private static final Logger log = LoggerFactory.getLogger(ProxyDescriptorParser.class);

    private final XMLInputFactory xmlInputFactory;

    public ProxyDescriptorParser() {
        this.xmlInputFactory = XMLInputFactory.newInstance();
        this.xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        this.xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    }

    /**
     * Parses the proxy descriptor and updates the bundle with metadata.
     */
    public void parse(InputStream inputStream, ApigeeBundle bundle) throws XMLStreamException {
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(inputStream);

        try {
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String elementName = reader.getLocalName();
                    
                    switch (elementName) {
                        case "APIProxy":
                            String name = reader.getAttributeValue(null, "name");
                            String revision = reader.getAttributeValue(null, "revision");
                            if (name != null) {
                                bundle.setName(name);
                            }
                            if (revision != null) {
                                try {
                                    bundle.setRevision(Integer.parseInt(revision));
                                } catch (NumberFormatException e) {
                                    log.warn("Invalid revision number: {}", revision);
                                }
                            }
                            break;
                        case "Description":
                            bundle.setDescription(readElementText(reader));
                            break;
                        case "CreatedAt":
                            String createdAt = readElementText(reader);
                            try {
                                bundle.setCreatedAt(Long.parseLong(createdAt));
                            } catch (NumberFormatException e) {
                                log.debug("Could not parse CreatedAt: {}", createdAt);
                            }
                            break;
                        case "BasePath":
                        case "BasePaths":
                            String basePath = readElementText(reader);
                            if (basePath != null && !basePath.isEmpty()) {
                                bundle.setBasePath(basePath);
                            }
                            break;
                    }
                }
            }
        } finally {
            reader.close();
        }
    }

    private String readElementText(XMLStreamReader reader) throws XMLStreamException {
        StringBuilder text = new StringBuilder();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                text.append(reader.getText());
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        return text.toString().trim();
    }
}

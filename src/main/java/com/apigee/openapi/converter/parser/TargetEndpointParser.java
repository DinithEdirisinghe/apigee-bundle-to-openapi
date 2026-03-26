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

import com.apigee.openapi.converter.model.Flow;
import com.apigee.openapi.converter.model.Step;
import com.apigee.openapi.converter.model.TargetEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for Apigee TargetEndpoint XML files.
 */
public class TargetEndpointParser {

    private static final Logger log = LoggerFactory.getLogger(TargetEndpointParser.class);

    private final XMLInputFactory xmlInputFactory;

    public TargetEndpointParser() {
        this.xmlInputFactory = XMLInputFactory.newInstance();
        this.xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        this.xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    }

    /**
     * Parses a TargetEndpoint from an XML input stream.
     */
    public TargetEndpoint parse(InputStream inputStream) throws XMLStreamException {
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(inputStream);
        TargetEndpoint endpoint = new TargetEndpoint();

        try {
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String elementName = reader.getLocalName();
                    
                    switch (elementName) {
                        case "TargetEndpoint":
                            endpoint.setName(reader.getAttributeValue(null, "name"));
                            break;
                        case "Description":
                            endpoint.setDescription(readElementText(reader));
                            break;
                        case "PreFlow":
                            endpoint.setPreFlow(parseFlow(reader, "PreFlow"));
                            break;
                        case "PostFlow":
                            endpoint.setPostFlow(parseFlow(reader, "PostFlow"));
                            break;
                        case "HTTPTargetConnection":
                            parseHttpTargetConnection(reader, endpoint);
                            break;
                        case "LoadBalancer":
                            parseLoadBalancer(reader, endpoint);
                            break;
                    }
                }
            }
        } finally {
            reader.close();
        }

        return endpoint;
    }

    private Flow parseFlow(XMLStreamReader reader, String endElement) throws XMLStreamException {
        Flow flow = new Flow();
        flow.setName(reader.getAttributeValue(null, "name"));

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String elementName = reader.getLocalName();
                switch (elementName) {
                    case "Description":
                        flow.setDescription(readElementText(reader));
                        break;
                    case "Request":
                        flow.setRequestSteps(parseSteps(reader, "Request"));
                        break;
                    case "Response":
                        flow.setResponseSteps(parseSteps(reader, "Response"));
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals(endElement)) {
                    break;
                }
            }
        }

        return flow;
    }

    private List<Step> parseSteps(XMLStreamReader reader, String endElement) throws XMLStreamException {
        List<Step> steps = new ArrayList<>();

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (reader.getLocalName().equals("Step")) {
                    steps.add(parseStep(reader));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals(endElement)) {
                    break;
                }
            }
        }

        return steps;
    }

    private Step parseStep(XMLStreamReader reader) throws XMLStreamException {
        Step step = new Step();

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String elementName = reader.getLocalName();
                switch (elementName) {
                    case "Name":
                        String name = readElementText(reader);
                        step.setName(name);
                        step.setPolicyName(name);
                        break;
                    case "Condition":
                        step.setCondition(readElementText(reader));
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals("Step")) {
                    break;
                }
            }
        }

        return step;
    }

    private void parseHttpTargetConnection(XMLStreamReader reader, TargetEndpoint endpoint) 
            throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String elementName = reader.getLocalName();
                switch (elementName) {
                    case "URL":
                        endpoint.setUrl(readElementText(reader));
                        break;
                    case "SSLInfo":
                        endpoint.setSslEnabled(true);
                        skipElement(reader, "SSLInfo");
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals("HTTPTargetConnection")) {
                    break;
                }
            }
        }
    }

    private void parseLoadBalancer(XMLStreamReader reader, TargetEndpoint endpoint) 
            throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String elementName = reader.getLocalName();
                if (elementName.equals("Server")) {
                    // Get the first server URL as the primary
                    String serverName = reader.getAttributeValue(null, "name");
                    endpoint.setLoadBalancerConfigName(serverName);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals("LoadBalancer")) {
                    break;
                }
            }
        }
    }

    private void skipElement(XMLStreamReader reader, String elementName) throws XMLStreamException {
        int depth = 1;
        while (reader.hasNext() && depth > 0) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                depth++;
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                depth--;
            }
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

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
import com.apigee.openapi.converter.model.ProxyEndpoint;
import com.apigee.openapi.converter.model.RouteRule;
import com.apigee.openapi.converter.model.Step;
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
 * Parser for Apigee ProxyEndpoint XML files.
 */
public class ProxyEndpointParser {

    private static final Logger log = LoggerFactory.getLogger(ProxyEndpointParser.class);

    private final XMLInputFactory xmlInputFactory;
    private final FlowConditionParser conditionParser;

    public ProxyEndpointParser() {
        this.xmlInputFactory = XMLInputFactory.newInstance();
        this.xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        this.xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        this.conditionParser = new FlowConditionParser();
    }

    /**
     * Parses a ProxyEndpoint from an XML input stream.
     */
    public ProxyEndpoint parse(InputStream inputStream) throws XMLStreamException {
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(inputStream);
        ProxyEndpoint endpoint = new ProxyEndpoint();

        try {
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String elementName = reader.getLocalName();
                    
                    switch (elementName) {
                        case "ProxyEndpoint":
                            endpoint.setName(reader.getAttributeValue(null, "name"));
                            break;
                        case "Description":
                            endpoint.setDescription(readElementText(reader));
                            break;
                        case "PreFlow":
                            endpoint.setPreFlow(parseFlow(reader, "PreFlow", Flow.FlowType.PRE_FLOW));
                            break;
                        case "PostFlow":
                            endpoint.setPostFlow(parseFlow(reader, "PostFlow", Flow.FlowType.POST_FLOW));
                            break;
                        case "Flows":
                            endpoint.setFlows(parseFlows(reader));
                            break;
                        case "FaultRules":
                            endpoint.setFaultRules(parseFaultRules(reader));
                            break;
                        case "DefaultFaultRule":
                            endpoint.setDefaultFaultRule(parseDefaultFaultRule(reader));
                            break;
                        case "HTTPProxyConnection":
                            parseHttpProxyConnection(reader, endpoint);
                            break;
                        case "RouteRule":
                            endpoint.addRouteRule(parseRouteRule(reader));
                            break;
                    }
                }
            }
        } finally {
            reader.close();
        }

        return endpoint;
    }

    private Flow parseFlow(XMLStreamReader reader, String endElement, Flow.FlowType flowType) 
            throws XMLStreamException {
        Flow flow = new Flow();
        flow.setFlowType(flowType);
        flow.setName(reader.getAttributeValue(null, "name"));

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String elementName = reader.getLocalName();
                switch (elementName) {
                    case "Description":
                        flow.setDescription(readElementText(reader));
                        break;
                    case "Condition":
                        String condition = readElementText(reader);
                        flow.setCondition(condition);
                        conditionParser.parseCondition(condition, flow);
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

    private List<Flow> parseFlows(XMLStreamReader reader) throws XMLStreamException {
        List<Flow> flows = new ArrayList<>();

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (reader.getLocalName().equals("Flow")) {
                    Flow flow = parseFlow(reader, "Flow", Flow.FlowType.CONDITIONAL);
                    flows.add(flow);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals("Flows")) {
                    break;
                }
            }
        }

        return flows;
    }

    private List<Flow> parseFaultRules(XMLStreamReader reader) throws XMLStreamException {
        List<Flow> faultRules = new ArrayList<>();

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (reader.getLocalName().equals("FaultRule")) {
                    Flow faultRule = parseFlow(reader, "FaultRule", Flow.FlowType.FAULT_RULE);
                    faultRules.add(faultRule);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals("FaultRules")) {
                    break;
                }
            }
        }

        return faultRules;
    }

    private Flow parseDefaultFaultRule(XMLStreamReader reader) throws XMLStreamException {
        return parseFlow(reader, "DefaultFaultRule", Flow.FlowType.DEFAULT_FAULT_RULE);
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

    private void parseHttpProxyConnection(XMLStreamReader reader, ProxyEndpoint endpoint) 
            throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String elementName = reader.getLocalName();
                switch (elementName) {
                    case "BasePath":
                        endpoint.setBasePath(readElementText(reader));
                        break;
                    case "VirtualHost":
                        endpoint.addVirtualHost(readElementText(reader));
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals("HTTPProxyConnection")) {
                    break;
                }
            }
        }
    }

    private RouteRule parseRouteRule(XMLStreamReader reader) throws XMLStreamException {
        RouteRule routeRule = new RouteRule();
        routeRule.setName(reader.getAttributeValue(null, "name"));

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String elementName = reader.getLocalName();
                switch (elementName) {
                    case "Condition":
                        routeRule.setCondition(readElementText(reader));
                        break;
                    case "TargetEndpoint":
                        routeRule.setTargetEndpoint(readElementText(reader));
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals("RouteRule")) {
                    break;
                }
            }
        }

        return routeRule;
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

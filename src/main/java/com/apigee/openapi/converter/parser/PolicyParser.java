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

import com.apigee.openapi.converter.model.Policy;
import com.apigee.openapi.converter.model.Policy.ParameterInfo;
import com.apigee.openapi.converter.model.Policy.PolicyType;
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
 * Parser for Apigee Policy XML files.
 * Extracts policy type and configuration from various policy types.
 */
public class PolicyParser {

    private static final Logger log = LoggerFactory.getLogger(PolicyParser.class);

    private final XMLInputFactory xmlInputFactory;

    public PolicyParser() {
        this.xmlInputFactory = XMLInputFactory.newInstance();
        this.xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        this.xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    }

    /**
     * Parses a Policy from an XML input stream.
     */
    public Policy parse(InputStream inputStream) throws XMLStreamException {
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(inputStream);
        Policy policy = new Policy();

        try {
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String elementName = reader.getLocalName();
                    
                    // Determine policy type from root element
                    PolicyType type = detectPolicyType(elementName);
                    policy.setType(type);
                    policy.setName(reader.getAttributeValue(null, "name"));
                    
                    String enabled = reader.getAttributeValue(null, "enabled");
                    policy.setEnabled(enabled == null || !"false".equalsIgnoreCase(enabled));
                    
                    String continueOnError = reader.getAttributeValue(null, "continueOnError");
                    policy.setContinueOnError("true".equalsIgnoreCase(continueOnError));
                    
                    // Parse type-specific content
                    switch (type) {
                        case EXTRACT_VARIABLES:
                            parseExtractVariables(reader, policy);
                            break;
                        case ASSIGN_MESSAGE:
                            parseAssignMessage(reader, policy);
                            break;
                        case VERIFY_API_KEY:
                            parseVerifyApiKey(reader, policy);
                            break;
                        case OAUTH_V2:
                            parseOAuthV2(reader, policy);
                            break;
                        case QUOTA:
                            parseQuota(reader, policy);
                            break;
                        case SPIKE_ARREST:
                            parseSpikeArrest(reader, policy);
                            break;
                        default:
                            parseGenericPolicy(reader, policy, elementName);
                            break;
                    }
                    break; // Only process the root element
                }
            }
        } finally {
            reader.close();
        }

        return policy;
    }

    private PolicyType detectPolicyType(String elementName) {
        switch (elementName) {
            case "ExtractVariables":
                return PolicyType.EXTRACT_VARIABLES;
            case "AssignMessage":
                return PolicyType.ASSIGN_MESSAGE;
            case "VerifyAPIKey":
                return PolicyType.VERIFY_API_KEY;
            case "OAuthV2":
                return PolicyType.OAUTH_V2;
            case "BasicAuthentication":
                return PolicyType.BASIC_AUTH;
            case "Quota":
                return PolicyType.QUOTA;
            case "SpikeArrest":
                return PolicyType.SPIKE_ARREST;
            case "ResponseCache":
            case "PopulateCache":
            case "LookupCache":
                return PolicyType.RESPONSE_CACHE;
            case "RaiseFault":
                return PolicyType.RAISE_FAULT;
            case "JavaScript":
                return PolicyType.JAVASCRIPT;
            case "ServiceCallout":
                return PolicyType.SERVICE_CALLOUT;
            case "RegularExpressionProtection":
                return PolicyType.REGEX_PROTECTION;
            case "XMLToJSON":
                return PolicyType.XML_TO_JSON;
            case "JSONToXML":
                return PolicyType.JSON_TO_XML;
            default:
                return PolicyType.UNKNOWN;
        }
    }

    private void parseExtractVariables(XMLStreamReader reader, Policy policy) throws XMLStreamException {
        String rootElement = reader.getLocalName();
        String source = null;
        String variablePrefix = "";
        List<ParameterInfo> params = new ArrayList<>();

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String elementName = reader.getLocalName();
                switch (elementName) {
                    case "DisplayName":
                        policy.setDisplayName(readElementText(reader));
                        break;
                    case "Source":
                        source = readElementText(reader);
                        policy.setConfigValue("source", source);
                        break;
                    case "VariablePrefix":
                        variablePrefix = readElementText(reader);
                        policy.setConfigValue("variablePrefix", variablePrefix);
                        break;
                    case "URIPath":
                        params.addAll(parseUriPathPatterns(reader, variablePrefix));
                        break;
                    case "QueryParam":
                        ParameterInfo queryParam = parseQueryParam(reader);
                        if (queryParam != null) {
                            params.add(queryParam);
                        }
                        break;
                    case "Header":
                        ParameterInfo headerParam = parseHeaderParam(reader);
                        if (headerParam != null) {
                            params.add(headerParam);
                        }
                        break;
                    case "FormParam":
                        ParameterInfo formParam = parseFormParam(reader);
                        if (formParam != null) {
                            params.add(formParam);
                        }
                        break;
                    case "JSONPayload":
                        params.addAll(parseJsonPayload(reader));
                        break;
                    case "XMLPayload":
                        params.addAll(parseXmlPayload(reader));
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals(rootElement)) {
                    break;
                }
            }
        }

        // Add all extracted parameters to policy
        for (ParameterInfo param : params) {
            policy.addExtractedParameter(param);
        }
    }

    private List<ParameterInfo> parseUriPathPatterns(XMLStreamReader reader, String variablePrefix) 
            throws XMLStreamException {
        List<ParameterInfo> params = new ArrayList<>();

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (reader.getLocalName().equals("Pattern")) {
                    String ignoreCase = reader.getAttributeValue(null, "ignoreCase");
                    String pattern = readElementText(reader);
                    
                    // Extract path parameters from pattern like /pets/{petId}
                    List<String> pathParams = extractPathParams(pattern);
                    for (String paramName : pathParams) {
                        ParameterInfo param = new ParameterInfo(paramName, ParameterInfo.Location.PATH);
                        param.setPattern(pattern);
                        param.setRequired(true);
                        if (!variablePrefix.isEmpty()) {
                            param.setVariableName(variablePrefix + "." + paramName);
                        }
                        params.add(param);
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals("URIPath")) {
                    break;
                }
            }
        }

        return params;
    }

    private List<String> extractPathParams(String pattern) {
        List<String> params = new ArrayList<>();
        int start = 0;
        while ((start = pattern.indexOf('{', start)) != -1) {
            int end = pattern.indexOf('}', start);
            if (end > start) {
                params.add(pattern.substring(start + 1, end));
                start = end + 1;
            } else {
                break;
            }
        }
        return params;
    }

    private ParameterInfo parseQueryParam(XMLStreamReader reader) throws XMLStreamException {
        String paramName = reader.getAttributeValue(null, "name");
        ParameterInfo param = new ParameterInfo(paramName, ParameterInfo.Location.QUERY);

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (reader.getLocalName().equals("Pattern")) {
                    param.setPattern(readElementText(reader));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals("QueryParam")) {
                    break;
                }
            }
        }

        return param;
    }

    private ParameterInfo parseHeaderParam(XMLStreamReader reader) throws XMLStreamException {
        String paramName = reader.getAttributeValue(null, "name");
        ParameterInfo param = new ParameterInfo(paramName, ParameterInfo.Location.HEADER);

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (reader.getLocalName().equals("Pattern")) {
                    param.setPattern(readElementText(reader));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals("Header")) {
                    break;
                }
            }
        }

        return param;
    }

    private ParameterInfo parseFormParam(XMLStreamReader reader) throws XMLStreamException {
        String paramName = reader.getAttributeValue(null, "name");
        ParameterInfo param = new ParameterInfo(paramName, ParameterInfo.Location.FORM);

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (reader.getLocalName().equals("Pattern")) {
                    param.setPattern(readElementText(reader));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals("FormParam")) {
                    break;
                }
            }
        }

        return param;
    }

    private List<ParameterInfo> parseJsonPayload(XMLStreamReader reader) throws XMLStreamException {
        List<ParameterInfo> params = new ArrayList<>();

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (reader.getLocalName().equals("Variable")) {
                    String varName = reader.getAttributeValue(null, "name");
                    String jsonPath = null;
                    
                    while (reader.hasNext()) {
                        int e = reader.next();
                        if (e == XMLStreamConstants.START_ELEMENT) {
                            if (reader.getLocalName().equals("JSONPath")) {
                                jsonPath = readElementText(reader);
                            }
                        } else if (e == XMLStreamConstants.END_ELEMENT) {
                            if (reader.getLocalName().equals("Variable")) {
                                break;
                            }
                        }
                    }
                    
                    if (varName != null && jsonPath != null) {
                        ParameterInfo param = new ParameterInfo(varName, ParameterInfo.Location.BODY);
                        param.setPattern(jsonPath);
                        params.add(param);
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals("JSONPayload")) {
                    break;
                }
            }
        }

        return params;
    }

    private List<ParameterInfo> parseXmlPayload(XMLStreamReader reader) throws XMLStreamException {
        List<ParameterInfo> params = new ArrayList<>();

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (reader.getLocalName().equals("Variable")) {
                    String varName = reader.getAttributeValue(null, "name");
                    String xpath = null;
                    
                    while (reader.hasNext()) {
                        int e = reader.next();
                        if (e == XMLStreamConstants.START_ELEMENT) {
                            if (reader.getLocalName().equals("XPath")) {
                                xpath = readElementText(reader);
                            }
                        } else if (e == XMLStreamConstants.END_ELEMENT) {
                            if (reader.getLocalName().equals("Variable")) {
                                break;
                            }
                        }
                    }
                    
                    if (varName != null && xpath != null) {
                        ParameterInfo param = new ParameterInfo(varName, ParameterInfo.Location.BODY);
                        param.setPattern(xpath);
                        params.add(param);
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals("XMLPayload")) {
                    break;
                }
            }
        }

        return params;
    }

    private void parseAssignMessage(XMLStreamReader reader, Policy policy) throws XMLStreamException {
        String rootElement = reader.getLocalName();
        List<ParameterInfo> headers = new ArrayList<>();
        List<ParameterInfo> queryParams = new ArrayList<>();

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String elementName = reader.getLocalName();
                switch (elementName) {
                    case "DisplayName":
                        policy.setDisplayName(readElementText(reader));
                        break;
                    case "AssignTo":
                        String createNew = reader.getAttributeValue(null, "createNew");
                        String type = reader.getAttributeValue(null, "type");
                        policy.setConfigValue("assignToCreateNew", "true".equals(createNew));
                        policy.setConfigValue("assignToType", type);
                        break;
                    case "Set":
                    case "Add":
                    case "Copy":
                        parseAssignMessageContent(reader, elementName, headers, queryParams);
                        break;
                    case "Remove":
                        skipElement(reader, "Remove");
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals(rootElement)) {
                    break;
                }
            }
        }

        // Store extracted headers and query params
        policy.setConfigValue("headers", headers);
        policy.setConfigValue("queryParams", queryParams);
        
        for (ParameterInfo header : headers) {
            policy.addExtractedParameter(header);
        }
        for (ParameterInfo qp : queryParams) {
            policy.addExtractedParameter(qp);
        }
    }

    private void parseAssignMessageContent(XMLStreamReader reader, String containerElement,
                                          List<ParameterInfo> headers, List<ParameterInfo> queryParams) 
            throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String elementName = reader.getLocalName();
                switch (elementName) {
                    case "Headers":
                        parseAssignHeaders(reader, headers);
                        break;
                    case "QueryParams":
                        parseAssignQueryParams(reader, queryParams);
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals(containerElement)) {
                    break;
                }
            }
        }
    }

    private void parseAssignHeaders(XMLStreamReader reader, List<ParameterInfo> headers) 
            throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (reader.getLocalName().equals("Header")) {
                    String name = reader.getAttributeValue(null, "name");
                    String value = readElementText(reader);
                    ParameterInfo param = new ParameterInfo(name, ParameterInfo.Location.HEADER);
                    param.setPattern(value);
                    headers.add(param);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals("Headers")) {
                    break;
                }
            }
        }
    }

    private void parseAssignQueryParams(XMLStreamReader reader, List<ParameterInfo> queryParams) 
            throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (reader.getLocalName().equals("QueryParam")) {
                    String name = reader.getAttributeValue(null, "name");
                    String value = readElementText(reader);
                    ParameterInfo param = new ParameterInfo(name, ParameterInfo.Location.QUERY);
                    param.setPattern(value);
                    queryParams.add(param);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals("QueryParams")) {
                    break;
                }
            }
        }
    }

    private void parseVerifyApiKey(XMLStreamReader reader, Policy policy) throws XMLStreamException {
        String rootElement = reader.getLocalName();

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String elementName = reader.getLocalName();
                switch (elementName) {
                    case "DisplayName":
                        policy.setDisplayName(readElementText(reader));
                        break;
                    case "APIKey":
                        String ref = reader.getAttributeValue(null, "ref");
                        policy.setConfigValue("apiKeyRef", ref);
                        
                        // Determine API key location from ref
                        if (ref != null) {
                            if (ref.contains("header")) {
                                policy.setConfigValue("apiKeyLocation", "header");
                                String headerName = extractRefName(ref);
                                policy.setConfigValue("apiKeyName", headerName);
                            } else if (ref.contains("queryparam")) {
                                policy.setConfigValue("apiKeyLocation", "query");
                                String paramName = extractRefName(ref);
                                policy.setConfigValue("apiKeyName", paramName);
                            }
                        }
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals(rootElement)) {
                    break;
                }
            }
        }
    }

    private String extractRefName(String ref) {
        // Extract parameter name from ref like "request.header.apikey" or "request.queryparam.apikey"
        int lastDot = ref.lastIndexOf('.');
        if (lastDot > 0) {
            return ref.substring(lastDot + 1);
        }
        return ref;
    }

    private void parseOAuthV2(XMLStreamReader reader, Policy policy) throws XMLStreamException {
        String rootElement = reader.getLocalName();

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String elementName = reader.getLocalName();
                switch (elementName) {
                    case "DisplayName":
                        policy.setDisplayName(readElementText(reader));
                        break;
                    case "Operation":
                        policy.setConfigValue("operation", readElementText(reader));
                        break;
                    case "AccessToken":
                        policy.setConfigValue("accessTokenRef", readElementText(reader));
                        break;
                    case "GrantType":
                        policy.setConfigValue("grantType", readElementText(reader));
                        break;
                    case "SupportedGrantTypes":
                        parseSupportedGrantTypes(reader, policy);
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals(rootElement)) {
                    break;
                }
            }
        }
    }

    private void parseSupportedGrantTypes(XMLStreamReader reader, Policy policy) throws XMLStreamException {
        List<String> grantTypes = new ArrayList<>();
        
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (reader.getLocalName().equals("GrantType")) {
                    grantTypes.add(readElementText(reader));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals("SupportedGrantTypes")) {
                    break;
                }
            }
        }
        
        policy.setConfigValue("supportedGrantTypes", grantTypes);
    }

    private void parseQuota(XMLStreamReader reader, Policy policy) throws XMLStreamException {
        String rootElement = reader.getLocalName();

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String elementName = reader.getLocalName();
                switch (elementName) {
                    case "DisplayName":
                        policy.setDisplayName(readElementText(reader));
                        break;
                    case "Allow":
                        String count = reader.getAttributeValue(null, "count");
                        if (count != null) {
                            policy.setConfigValue("allowCount", Integer.parseInt(count));
                        }
                        break;
                    case "Interval":
                        policy.setConfigValue("interval", readElementText(reader));
                        break;
                    case "TimeUnit":
                        policy.setConfigValue("timeUnit", readElementText(reader));
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals(rootElement)) {
                    break;
                }
            }
        }
    }

    private void parseSpikeArrest(XMLStreamReader reader, Policy policy) throws XMLStreamException {
        String rootElement = reader.getLocalName();

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String elementName = reader.getLocalName();
                switch (elementName) {
                    case "DisplayName":
                        policy.setDisplayName(readElementText(reader));
                        break;
                    case "Rate":
                        policy.setConfigValue("rate", readElementText(reader));
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals(rootElement)) {
                    break;
                }
            }
        }
    }

    private void parseGenericPolicy(XMLStreamReader reader, Policy policy, String rootElement) 
            throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (reader.getLocalName().equals("DisplayName")) {
                    policy.setDisplayName(readElementText(reader));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (reader.getLocalName().equals(rootElement)) {
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

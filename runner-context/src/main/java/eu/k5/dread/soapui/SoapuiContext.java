package eu.k5.dread.soapui;

import com.eviware.soapui.SoapUI;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

// https://github.com/denninger-eu/dread-karate-context

/**
 * BSD 2-Clause License
 * <p>
 * Copyright (c) 2020, Frank Denninger
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p>
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public class SoapuiContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoapuiContext.class);
    private static final String GLOBAL = "Global";
    private static final String PROJECT = "Project";
    private static final String TEST_SUITE = "TestSuite";
    private static final String TEST_CASE = "TestCase";
    private final Map<String, PropertyHolder> propertyHolders = new HashMap<>();

    private final CompositePropertyHolder compositePropertyHolder = new CompositePropertyHolder("");

    private ScriptEngineManager scriptEngineManager;
    private ScriptEngine scriptEngine;
    private Object testcase;


    public SoapuiContext() {
        this(null);
    }

    public SoapuiContext(Object testcase) {
        this.testcase = testcase;
        propertyHolders.put(toLowerCase(GLOBAL), SoapUI.getGlobalProperties());
        propertyHolders.put(toLowerCase(PROJECT), new PropertyHolder(PROJECT));
        propertyHolders.put(toLowerCase(TEST_SUITE), new PropertyHolder(TEST_SUITE));
        propertyHolders.put(toLowerCase(TEST_CASE), new PropertyHolder(TEST_CASE));
        propertyHolders.put("", compositePropertyHolder);
    }

    public String sleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "";
    }

    public String read(String fileName) {
        if (testcase == null) {
            throw new IllegalStateException("Testcase required to read files");
        }
        try (InputStream is = testcase.getClass().getResourceAsStream(fileName)) {
            if (is == null) {
                throw new IllegalArgumentException("File '" + fileName + "' not found in package" + testcase.getClass().getPackage().toString());

            }
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return toString(reader);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read file: " + fileName);
        }
    }

    private synchronized ScriptEngineManager getScriptEngineManager() {
        if (scriptEngineManager == null) {
            scriptEngineManager = new ScriptEngineManager();
        }
        return scriptEngineManager;
    }

    private synchronized ScriptEngine getScriptEngine() {
        if (scriptEngine == null) {
            scriptEngine = getScriptEngineManager().getEngineByName("groovy");
            ProjectWrapper project = new ProjectWrapper(propertyHolders.get(toLowerCase(PROJECT)));
            TestSuiteWrapper testSuite = new TestSuiteWrapper(project, propertyHolders.get(toLowerCase(TEST_SUITE)));
            TestCaseWrapper testCase = new TestCaseWrapper(this, testSuite, propertyHolders.get(toLowerCase(TEST_CASE)));
            scriptEngine.put("testRunner", new TestRunner(this, testCase));
            scriptEngine.put("log", resolveLogger());
            scriptEngine.put("context", new Object() {
                public String expand(String value) {
                    return SoapuiContext.this.expand(value);
                }
            });
        }
        return scriptEngine;
    }

    private static Object resolveLogger() {
        try {
            Class<?> log4j = Class.forName("org.apache.log4j.Logger");
            Method factoryMethod = log4j.getDeclaredMethod("getRootLogger");
            return factoryMethod.invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LoggerFactory.getLogger(SoapuiContext.class).info("Using slf4j");
        }
        return LoggerFactory.getLogger(SoapuiContext.class);
    }

    public static class TestRunner {
        private final SoapuiContext runnerContext;
        private final TestCaseWrapper testCase;

        TestRunner(SoapuiContext runnerContext, TestCaseWrapper testCase) {
            this.runnerContext = runnerContext;
            this.testCase = testCase;
        }

        public TestCaseWrapper getTestCase() {
            return testCase;
        }
    }

    interface SoapUiWrapper {
        PropertyHolder getPropertyHolder();

        default String getPropertyValue(String name) {
            Property property = getPropertyHolder().getProperty(name);
            if (property == null) {
                return null;
            }
            return property.value;
        }

        default void setPropertyValue(String propertyName, String value) {
            getPropertyHolder().setProperty(propertyName, value);
        }
    }

    public static class TestCaseWrapper implements SoapUiWrapper {
        private final TestSuiteWrapper testSuite;
        private final PropertyHolder propertyHolder;
        private final SoapuiContext runnerContext;

        private Map<String, TestStepWrapper> testSteps;

        TestCaseWrapper(SoapuiContext runnerContext, TestSuiteWrapper testSuite, PropertyHolder propertyHolder) {
            this.runnerContext = runnerContext;
            this.testSuite = testSuite;
            this.propertyHolder = propertyHolder;
        }

        @Override
        public PropertyHolder getPropertyHolder() {
            return propertyHolder;
        }

        public TestSuiteWrapper getTestSuite() {
            return testSuite;
        }

        public Map<String, TestStepWrapper> getTestSteps() {
            if (testSteps == null) {
                testSteps = new HashMap<>();
                for (PropertyHolder step : runnerContext.propertyHolders.values()) {
                    TestStepWrapper wrapper = new TestStepWrapper(step);
                    testSteps.put(step.getName(), wrapper);
                }
            }
            return testSteps;
        }
    }

    // replicates com.eviware.soapui.impl.wsdl.teststeps.RestTestRequestStep
    public static class TestStepWrapper implements SoapUiWrapper {
        private final PropertyHolder propertyHolder;

        public TestStepWrapper(PropertyHolder propertyHolder) {
            this.propertyHolder = propertyHolder;
        }

        @Override
        public PropertyHolder getPropertyHolder() {
            return propertyHolder;
        }

        public HttpRequestWrapper getHttpRequest() {
            return new HttpRequestWrapper(propertyHolder);
        }

        public HttpRequestWrapper getTestRequest() {
            return new HttpRequestWrapper(propertyHolder);
        }
    }


    // replicates com.eviware.soapui.impl.wsdl.teststeps.RestTestRequest
    public static class HttpRequestWrapper {
        private final PropertyHolder propertyHolder;

        HttpRequestWrapper(PropertyHolder propertyHolder) {
            this.propertyHolder = propertyHolder;
        }


        public String getRequestContent() {
            return propertyHolder.getPropertyValue(RestRequestContext.REQUEST);
        }

        public void setRequestContent(String value) {
            propertyHolder.setPropertyValue(RestRequestContext.REQUEST, value);
        }

        public HttpResponseWrapper getResponse() {
            return new HttpResponseWrapper(propertyHolder);
        }

    }

    public static class HttpResponseWrapper {

        private PropertyHolder propertyHolder;

        public HttpResponseWrapper(PropertyHolder propertyHolder) {
            this.propertyHolder = propertyHolder;
        }

        public String getResponseContent() {
            return propertyHolder.getPropertyValue(RestRequestContext.RESPONSE);
        }

        public void setResponseContent(String value) {
            propertyHolder.setPropertyValue(RestRequestContext.RESPONSE, value);
        }
    }

    public static class TestSuiteWrapper implements SoapUiWrapper {
        private final ProjectWrapper project;
        private final PropertyHolder propertyHolder;


        TestSuiteWrapper(ProjectWrapper project, PropertyHolder propertyHolder) {
            this.project = project;
            this.propertyHolder = propertyHolder;
        }

        @Override
        public PropertyHolder getPropertyHolder() {
            return propertyHolder;
        }

        public ProjectWrapper getProject() {
            return project;
        }
    }

    public static class ProjectWrapper implements SoapUiWrapper {
        private final PropertyHolder propertyHolder;

        ProjectWrapper(PropertyHolder propertyHolder) {
            this.propertyHolder = propertyHolder;
        }

        @Override
        public PropertyHolder getPropertyHolder() {
            return propertyHolder;
        }
    }

    @SuppressWarnings("WeakerAccess") // Used from karate
    public RestRequestContext requestStep(String name) {
        if (propertyHolders.containsKey(name)) {
            return (RestRequestContext) propertyHolders.get(toLowerCase(name));
        }
        RestRequestContext request = new RestRequestContext(this, name);
        propertyHolders.put(toLowerCase(name), request);
        return request;
    }

    @SuppressWarnings("WeakerAccess") // Used from karate
    public PropertyHolder propertiesStep(String name) {
        if (propertyHolders.containsKey(name)) {
            throw new IllegalArgumentException("Context name already used");
        }
        PropertiesContext properties = new PropertiesContext(this, name);
        propertyHolders.put(name, properties);
        compositePropertyHolder.addDelegate(properties);
        return properties;
    }

    @SuppressWarnings("WeakerAccess") // Used from karate
    public Transfer transfer(String sourceProperty) {
        return new Transfer(this, new Query(sourceProperty, "", ""));
    }

    @SuppressWarnings("WeakerAccess") // Used from karate
    public Transfer transfer(String sourceProperty, String sourceExpression, String sourceLanguage) {
        return new Transfer(this, new Query(sourceProperty, sourceExpression, sourceLanguage));
    }


    public void setProperty(String property, String value) {
        updateOrCreateProperty(property, value);
    }

    @SuppressWarnings("WeakerAccess") // Used from karate
    public ScriptContext groovyScript(String name) {
        if (propertyHolders.containsKey(name)) {
            return (ScriptContext) propertyHolders.get(name);
        }
        ScriptContext scriptContext = new ScriptContext(this, name);
        propertyHolders.put(name, scriptContext);
        return scriptContext;
    }

    public static class Transfer {

        private final SoapuiContext context;
        private final Query source;

        Transfer(SoapuiContext context, Query source) {
            this.context = context;
            this.source = source;
        }

        @SuppressWarnings("WeakerAccess") // Used from karate
        public String to(String targetProperty) {
            return to(targetProperty, "", "");
        }

        @SuppressWarnings("WeakerAccess") // Used from karate
        public String to(String targetProperty, String targetExpression, String targetLanguage) {
            String value = context.resolveValue(source);
            LOGGER.info("Source {} resolved to {}", source, value);
            Query target = new Query(targetProperty, targetExpression, targetLanguage);
            context.updateValue(target, value);

            return "Transfer " + source + " to " + target + " '" + shorten(value, 50) + "'";

        }
    }

    private static String shorten(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.substring(0, Math.min(maxLength, value.length()));
    }


    private String resolveValue(Query query) {
        Property property = resolveProperty(query.getProperty());
        if (property == null || property.value == null) {
            return null;
        }
        if (query.getExpression() == null || query.getExpression().isEmpty()) {
            return property.getValue();
        }
        switch (query.getLanguage()) {
            case "JSONPATH":
                return extractJsonPath(property, query.getExpression());
            case "XPATH":
                return extractXPath(property, query.getExpression());

            default:
                throw new UnsupportedOperationException("Language not supported " + query.getLanguage());
        }
    }

    private String extractXPath(Property property, String expression) {
        try {
            Document document = property.asXmlDocument();
            XPath xPath = XPathFactory.newInstance().newXPath();
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(document, XPathConstants.NODESET);
            if (nodeList.getLength() == 1) {
                Node item = nodeList.item(0);
                if (item instanceof Element) {
                    return item.getFirstChild().getNodeValue();
                }
                return item.getNodeValue();
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void updateValue(Query target, String value) {
        if (target.getExpression() == null || target.getExpression().isEmpty()) {
            updateOrCreateProperty(target.getProperty(), value);
            return;
        }
        Property property = resolveProperty(target.getProperty());
        switch (target.getLanguage()) {
            case "JSONPATH":
                updateJsonPath(value, property, target.getExpression());
                return;

        }
        throw new UnsupportedOperationException();
    }

    private void updateJsonPath(String value, Property property, String expression) {
        if (value != null && value.trim().startsWith("{") && value.trim().endsWith("}")) {
            Object parse = JSONValue.parse(value);
            if (parse != null) {
                property.asJsonDocument().set(expression, parse);
            } else {
                property.asJsonDocument().set(expression, value);
            }
        } else {
            property.asJsonDocument().set(expression, value);
        }
        property.makeJsonMain();
    }

    private void updateOrCreateProperty(String property, String value) {
        Property resolvedProperty = resolveProperty(property);
        if (resolvedProperty == null) {
            resolvedProperty = createProperty(property);
        }
        resolvedProperty.setValue(value);
    }

    private String extractJsonPath(Property property, String expression) {
        try {

            Object read = property.asJsonDocument().read(JsonPath.compile(expression), JSONArray.class);

            // No result, doing some hacks outside the spec of jsonpath to try to read something
            if (read instanceof JSONArray && ((JSONArray) read).isEmpty()) {
                expression = expression.trim();
                if (expression.endsWith("[0]")) {
                    // removing just the last array access seems more stable than to remove all; try that first

                    // JsonPath sucks, you can't select the first entry from an result array in jsonpath itself.
                    // do it manually, using jsonpath ending with [0] as indicator to do it
                    // https://github.com/json-path/JsonPath/issues/272
                    Object temp = property.asJsonDocument().read(JsonPath.compile(expression.substring(0, expression.length() - 3)), JSONArray.class);
                    if (temp instanceof JSONArray && !((JSONArray) temp).isEmpty()) {
                        return ((JSONArray) temp).get(0).toString();
                    }
                }
                if (expression.contains("[0]")) {
                    // JsonPath sucks, you can't select the first entry from an result array in jsonpath itself.
                    // do it manually, using jsonpath with [0] as indicator to do it
                    // https://github.com/json-path/JsonPath/issues/272
                    Object temp = property.asJsonDocument().read(JsonPath.compile(expression.replace("[0]", "")), JSONArray.class);
                    if (temp instanceof JSONArray && !((JSONArray) temp).isEmpty()) {
                        return ((JSONArray) temp).get(0).toString();
                    }
                }
            }
            if (read != null) {
                return read.toString();
            } else {
                return "";
            }
        } catch (
                InvalidJsonException exception) {
            LOGGER.warn("Unable to parse json: {}", exception.getMessage(), exception);
            return "";
        } catch (
                InvalidPathException exception) {
            LOGGER.warn("Unable to parse jsonpath expression: {}", exception.getMessage(), exception);
            return "";

        }
    }

    private static final Pattern EXPRESSION = Pattern.compile("\\$\\{(?<expression>\\=?[-._#a-zA-Z0-9\"\\(\\)]*?)}");

    @SuppressWarnings("WeakerAccess") // Used from karate
    public String expand(String value) {
        if (value == null) {
            return "";
        } else if (value.isEmpty()) {
            return "";
        }

        StringBuilder resultBuilder = new StringBuilder();
        Matcher matcher = EXPRESSION.matcher(value);
        int start = 0;
        while (matcher.find()) {
            if (start != matcher.start()) {
                resultBuilder.append(value.subSequence(start, matcher.start()));
            }
            String expression = matcher.group("expression");
            resultBuilder.append(resolveExpression(expression));
            start = matcher.end();
        }
        if (start == 0) {
            return value;
        }
        if (start != value.length()) {
            resultBuilder.append(value.subSequence(start, value.length()));
        }
        return resultBuilder.toString();
    }

    private String resolveExpression(String expression) {
        if (expression.startsWith("=")) {
            try {
                Object result = getScriptEngine().eval(expression.substring(1));
                if (result == null) {
                    return "";
                }
                return result.toString();
            } catch (ScriptException e) {
                if (e.getCause() != null) {
                    if (e.getCause().getCause() != null) {
                        return e.getCause().getCause().getMessage();
                    }
                    return e.getCause().getMessage();
                }
                return e.getMessage();
            }
        }
        Property property = resolveProperty(expression);
        if (property == null) {
            return "";
        } else if (property.getExpandedValue() != null) {
            return property.getExpandedValue();
        } else {
            String expanded = expand(property.value);
            property.setExpandedValue(expanded);
            return expanded;
        }
    }


    String resolvePropertyValue(String propertyName) {
        Property property = resolveProperty(propertyName);
        if (property == null) {
            return null;
        }
        return property.value;
    }

    Property resolveProperty(String property) {
        String[] parts = property.split("#");
        if (parts.length == 1) {
            return compositePropertyHolder.getProperty(property);
        } else if (parts.length == 2) {
            return getPropertyHolder(parts[0]).getProperty(parts[1]);
        } else if (parts.length == 3) {
            PropertyHolder propertyHolder = getPropertyHolder(parts[1]);
            return propertyHolder.getProperty(parts[2]);
        }
        throw new IllegalArgumentException("unsupported format");
    }

    private Property createProperty(String property) {
        String[] parts = property.split("#");
        if (parts.length == 2) {
            return getPropertyHolder(parts[0]).createProperty(parts[1]);
        } else if (parts.length == 3) {
            return getPropertyHolder(parts[1]).createProperty(parts[2]);
        }
        throw new IllegalArgumentException("unsupported format");
    }

    private PropertyHolder getPropertyHolder(String name) {
        PropertyHolder propertyHolder = propertyHolders.get(toLowerCase(name));
        if (propertyHolder == null) {
            throw new IllegalArgumentException("Unable to get PropertyHolder: " + name);
        }
        return propertyHolder;
    }


    private static class Query {
        private final String property;
        private final String expression;
        private final String language;

        Query(String property, String expression, String language) {
            this.property = property;
            this.expression = expression;
            this.language = language;
        }

        String getProperty() {
            return property;
        }

        String getExpression() {
            return expression;
        }

        String getLanguage() {
            return language;
        }

        @Override
        public String toString() {
            if (expression == null && language == null) {
                return property;
            } else {
                return property + " " + language + "('" + expression + "')";
            }
        }
    }


    static class Property {
        private final String name;
        private String value;

        private String expandedValue;

        private DocumentContext asJson;
        private Document asXmlDocument;

        private Property(String name) {
            this.name = name;
        }

        DocumentContext asJsonDocument() {
            if (asJson == null) {
                asJson = JsonPath.parse(value);
            }
            return asJson;
        }

        public Document asXmlDocument() {
            if (asXmlDocument == null) {
                try {
                    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = builderFactory.newDocumentBuilder();
                    asXmlDocument = builder.parse(new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)));
                } catch (IOException | SAXException | ParserConfigurationException e) {
                    throw new RuntimeException("Unable to parse value", e);
                }
            }
            return asXmlDocument;
        }

        public void setValue(String value) {
            this.value = value;
            asJson = null;
            asXmlDocument = null;
            expandedValue = null;
        }

        public String getValue() {
            if (value == null) {
                if (asJson != null) {
                    value = asJson.jsonString();
                    expandedValue = null;
                }
            }
            return value;
        }

        public String getExpandedValue() {
            return expandedValue;
        }

        public void setExpandedValue(String expandedValue) {
            this.expandedValue = expandedValue;
        }

        void makeJsonMain() {
            value = null;
        }


    }

    public static class PropertyHolder {
        private final Map<String, Property> properties = new HashMap<>();
        private final String name;

        public PropertyHolder(String name) {
            this.name = name;
        }

        public void setProperty(String name, String value) {
            properties.computeIfAbsent(toLowerCase(name), Property::new).setValue(value);
        }

        public PropertyHolder property(String name, String value) {
            setProperty(name, value);
            return this;
        }

        public Property getProperty(String name) {
            return properties.get(toLowerCase(name));
        }

        public String getPropertyValue(String name) {
            Property property = getProperty(name);
            if (property != null) {
                return property.getValue();
            }
            return null;
        }

        public void setPropertyValue(String name, String value) {
            setProperty(name, value);
        }

        public Property getOrCreateProperty(String name) {
            return properties.computeIfAbsent(toLowerCase(name), this::createProperty);
        }


        public Property createProperty(String name) {
            Property property = new Property(name);
            properties.put(toLowerCase(name), property);
            return property;
        }

        public String getName() {
            return name;
        }

    }

    public static class StepContext extends PropertyHolder {
        private final SoapuiContext runnerContext;
        private final Map<String, String> properties = new HashMap<>();


        StepContext(SoapuiContext runnerContext, String name) {
            super(name);
            this.runnerContext = runnerContext;
        }

        String expand(Property property) {
            if (property == null) {
                return null;
            }
            return runnerContext.expand(property.getValue());
        }


        SoapuiContext getRunnerContext() {
            return runnerContext;
        }
    }

    public static class CompositePropertyHolder extends PropertyHolder {
        private PropertyHolder current = null;
        private List<PropertyHolder> delegates = new ArrayList<>();

        public CompositePropertyHolder(String name) {
            super(name);
        }

        @Override
        public Property getProperty(String name) {
            if (current != null) {
                Property property = current.getProperty(name);
                if (property != null) {
                    return property;
                }
            }
            for (PropertyHolder delegate : delegates) {
                Property property = delegate.getProperty(name);
                if (property != null) {
                    return property;
                }
            }
            return null;
        }

        public void setCurrent(PropertyHolder current) {
            this.current = current;
        }

        void addDelegate(PropertyHolder delegate) {
            delegates.add(delegate);
        }

        @Override
        public Property getOrCreateProperty(String name) {
            throw new UnsupportedOperationException("unsupported");
        }

        @Override
        public void setProperty(String name, String value) {
            throw new UnsupportedOperationException("unsupported");
        }
    }

    public static class PropertiesContext extends StepContext {

        PropertiesContext(SoapuiContext runnerContext, String name) {
            super(runnerContext, name);
        }
    }


    public static class RestRequestContext extends StepContext {
        static final String URL = "url";
        static final String REQUEST = "Request";
        static final String RESPONSE = "Response";
        static final String STATUS = "Status";

        RestRequestContext(SoapuiContext runnerContext, String name) {
            super(runnerContext, name);
        }

        @SuppressWarnings("WeakerAccess") // Used from karate
        public String url() {
            try {
                getRunnerContext().compositePropertyHolder.setCurrent(this);
                return expand(getProperty(URL));
            } finally {
                getRunnerContext().compositePropertyHolder.setCurrent(null);
            }
        }

        @SuppressWarnings("WeakerAccess") // Used from karate
        public RestRequestContext url(String newUrl) {
            setProperty(URL, newUrl);
            return this;
        }

        @SuppressWarnings("WeakerAccess") // Used from karate
        public String request() {
            return expand(getProperty(REQUEST));
        }

        @SuppressWarnings("WeakerAccess") // Used from karate
        public RestRequestContext request(String request) {
            setProperty(REQUEST, request);
            return this;
        }

        public RestRequestContext request(Object value) {
            if (value == null) {
                return this;
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked") JSONObject jsonObject = new JSONObject((Map<String, ?>) value);
                return request(jsonObject.toJSONString());
            }

            throw new IllegalArgumentException("Unsupported type for request: " + value.getClass().getName());
        }

        public String response() {
            return expand(getProperty(RESPONSE));
        }

        public RestRequestContext response(Object response) {
            if (response == null) {
                return this;
            } else if (response instanceof Map) {
                @SuppressWarnings("unchecked") JSONObject jsonObject = new JSONObject((Map) response);
                return response(jsonObject.toJSONString());
            } else if (response instanceof JSONArray) {
                return response(((JSONArray) response).toJSONString());
            }
            throw new IllegalArgumentException("Unsupported type for request: " + response.getClass().getName());
        }

        public RestRequestContext response(String response) {
            setProperty(RESPONSE, response);
            return this;
        }

        public RestRequestContext status(int status) {
            setProperty(STATUS, Integer.toString(status));
            return this;
        }

        @SuppressWarnings("WeakerAccess")
        public boolean assertJsonExists(String expression) {
            try {
                DocumentContext document = getProperty(RESPONSE).asJsonDocument();
                Object result = document.read(expression);
                return result != null;
            } catch (PathNotFoundException exception) {
                return false;
            }
        }


        @SuppressWarnings("WeakerAccess")
        public void assertJsonPathExists(String expression, String expected) {
            boolean exists = Boolean.parseBoolean(expected);
            boolean actual = assertJsonExists(expression);
            if (exists != actual) {
                throw new IllegalArgumentException("json path does not exists '" + expression + "' in \n" + response());
            }
        }

        @SuppressWarnings("WeakerAccess")
        public void assertStatus(String expected) {
            boolean match = statusAnyMatch(expected);
            if (!match) {
                throw new IllegalArgumentException("Expected  status " + expected + " actual " + Integer.valueOf(getProperty(STATUS).value) + " in \n" + response());
            }
        }

        @SuppressWarnings("WeakerAccess")
        public void assertInvalidStatus(String expected){
            boolean match = statusAnyMatch(expected);
            if (match) {
                throw new IllegalArgumentException("Expected  not status " + expected + " actual " + Integer.valueOf(getProperty(STATUS).value) + " in \n" + response());
            }
        }

        private boolean statusAnyMatch(String needle){
            Pattern pattern = Pattern.compile("(?<number>\\d+)", Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(needle);
            List<Integer> status = new ArrayList<>();
            while (matcher.find()) {
                try {
                    status.add(Integer.parseInt(matcher.group("number")));
                } catch (NumberFormatException exception) {
                    LOGGER.warn("Unable to parse number for status assert" + matcher.group("number"));
                }
            }
            int actualStatus = Integer.valueOf(getProperty(STATUS).value);

            return status.stream().anyMatch(s -> s == actualStatus);
        }

        public void assertStatus(int... status) {
            int actualStatus = Integer.valueOf(getProperty(STATUS).value);

            boolean match = IntStream.of(status).anyMatch(s -> s == actualStatus);
            if (!match) {
                throw new IllegalArgumentException("Expected status " + Arrays.toString(status) + " actual " + actualStatus + " in \n" + response());
            }
        }

        public void assertNotStatus(int... status) {
            int actualStatus = Integer.valueOf(getProperty(STATUS).value);

            boolean match = IntStream.of(status).anyMatch(s -> s == actualStatus);
            if (match) {
                throw new IllegalArgumentException("Expected not status " + Arrays.toString(status) + " actual " + actualStatus + " in \n" + response());
            }
        }

        public void readRequest(String s) {
        }
    }

    public static class ScriptContext extends StepContext {
        private static final String SCRIPT = "script";
        private static final String RESULT = "result";

        ScriptContext(SoapuiContext runnerContext, String name) {
            super(runnerContext, name);
        }

        public ScriptContext script(InputStream object) {
            try (Reader reader = new InputStreamReader(object, StandardCharsets.UTF_8)) {
                return script(reader);
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to read from inputstream", e);
            }
        }

        @SuppressWarnings("WeakerAccess") // Used from karate
        public ScriptContext script(Reader reader) {
            return script(SoapuiContext.toString(reader));
        }

        @SuppressWarnings("WeakerAccess") // Used from karate
        public ScriptContext script(String script) {
            setProperty(SCRIPT, script);
            return this;
        }

        public void result(String result) {
            setProperty(RESULT, result);
        }

        @SuppressWarnings("WeakerAccess") // Used from karate
        public String execute() {
            String result = doExecute();
            setProperty(RESULT, result);
            return result;
        }

        private String doExecute() {
            try {
                Property scriptProperty = getProperty(SCRIPT);

                Object result = getRunnerContext().getScriptEngine().eval(scriptProperty.value);
                if (result != null) {
                    return result.toString();
                }
                return "";
            } catch (ScriptException e) {
                if (e.getCause() != null) {
                    return e.getCause().getMessage();
                }
                return e.getMessage();
            }
        }

        public String result() {
            Property property = getProperty(RESULT);
            if (property == null) {
                return "";
            }
            return property.value;
        }

    }

    private static String toString(Reader reader) {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[4096];
        int length;
        try {
            while ((length = reader.read(buffer)) > 0) {
                builder.append(buffer, 0, length);
            }
            return builder.toString();
        } catch (IOException exc) {
            throw new IllegalArgumentException("Unable to read from reader", exc);
        }
    }

    private static String toLowerCase(String string) {
        if (string == null) {
            return null;
        }
        return string.toLowerCase();
    }
}




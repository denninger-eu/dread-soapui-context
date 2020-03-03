package eu.k5.dread.karate;

import com.eviware.soapui.SoapUI;
import com.jayway.jsonpath.*;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class RunnerContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunnerContext.class);
    private static final PropertyHolder EMPTY = new PropertyHolder("empty");
    private static final String GLOBAL = "Global";
    private static final String PROJECT = "Project";
    private static final String TEST_SUITE = "TestSuite";
    private static final String TEST_CASE = "TestCase";
    private final Map<String, PropertyHolder> propertyHolders = new HashMap<>();

    private final CompositePropertyHolder compositePropertyHolder = new CompositePropertyHolder("");

    private ScriptEngineManager scriptEngineManager;
    private ScriptEngine scriptEngine;

    public RunnerContext() {
        propertyHolders.put(GLOBAL, SoapUI.getGlobalProperties());
        propertyHolders.put(PROJECT, new PropertyHolder(PROJECT));
        propertyHolders.put(TEST_SUITE, new PropertyHolder(TEST_SUITE));
        propertyHolders.put(TEST_CASE, new PropertyHolder(TEST_CASE));
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

    private synchronized ScriptEngineManager getScriptEngineManager() {
        if (scriptEngineManager == null) {
            scriptEngineManager = new ScriptEngineManager();
        }
        return scriptEngineManager;
    }

    private synchronized ScriptEngine getScriptEngine() {
        if (scriptEngine == null) {
            scriptEngine = getScriptEngineManager().getEngineByName("groovy");
            ProjectWrapper project = new ProjectWrapper(propertyHolders.get(PROJECT));
            TestSuiteWrapper testSuite = new TestSuiteWrapper(project, propertyHolders.get(TEST_SUITE));
            TestCaseWrapper testCase = new TestCaseWrapper(this, testSuite, propertyHolders.get(TEST_CASE));
            scriptEngine.put("testRunner", new TestRunner(this, testCase));
            scriptEngine.put("log", resolveLogger());
        }
        return scriptEngine;
    }

    private static Object resolveLogger() {
        try {
            Class<?> log4j = Class.forName("org.apache.log4j.Logger");
            Method factoryMethod = log4j.getDeclaredMethod("getRootLogger");
            return factoryMethod.invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LoggerFactory.getLogger(RunnerContext.class).info("Using slf4j");
        }
        return LoggerFactory.getLogger(RunnerContext.class);
    }

    public static class TestRunner {
        private final RunnerContext runnerContext;
        private final TestCaseWrapper testCase;

        TestRunner(RunnerContext runnerContext, TestCaseWrapper testCase) {
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
        private final RunnerContext runnerContext;

        private Map<String, TestStepWrapper> testSteps;

        TestCaseWrapper(RunnerContext runnerContext, TestSuiteWrapper testSuite, PropertyHolder propertyHolder) {
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
            return new HttpRequestWrapper(propertyHolder.getOrCreateProperty(RestRequestContext.REQUEST));
        }
    }


    public static class HttpRequestWrapper {
        private final Property property;

        HttpRequestWrapper(Property property) {
            this.property = property;
        }

        public String getRequestContent() {
            return property.getValue();
        }

        public void setRequestContent(String value) {
            property.setValue(value);
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
        RestRequestContext request = new RestRequestContext(this, name);
        propertyHolders.put(name, request);
        return request;
    }

    @SuppressWarnings("WeakerAccess") // Used from karate
    public PropertyHolder propertiesStep(String name) {
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
        ScriptContext scriptContext = new ScriptContext(this, name);
        propertyHolders.put(name, scriptContext);
        return scriptContext;
    }

    public static class Transfer {

        private final RunnerContext context;
        private final Query source;

        Transfer(RunnerContext context, Query source) {
            this.context = context;
            this.source = source;
        }

        @SuppressWarnings("WeakerAccess") // Used from karate
        public void to(String targetProperty) {
            to(targetProperty, "", "");
        }

        @SuppressWarnings("WeakerAccess") // Used from karate
        public void to(String targetProperty, String targetExpression, String targetLanguage) {
            String value = context.resolveValue(source);
            LOGGER.info("Source {} resolved to {}", source, value);
            context.updateValue(new Query(targetProperty, targetExpression, targetLanguage), value);
        }
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
                return extractJsonPath(property.getValue(), query.getExpression());
            default:
                throw new UnsupportedOperationException("Language not supported " + query.getLanguage());
        }
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
        property.asJsonDocument().set(expression, value);
        property.makeJsonMain();
    }

    private void updateOrCreateProperty(String property, String value) {
        Property resolvedProperty = resolveProperty(property);
        if (resolvedProperty == null) {
            resolvedProperty = createProperty(property);
        }
        resolvedProperty.setValue(value);
    }

    private String extractJsonPath(String json, String expression) {
        try {
            return JsonPath.read(json, expression);
        } catch (InvalidJsonException exception) {
            LOGGER.warn("Unable to parse json: {}", exception.getMessage(), exception);
            return "";
        } catch (InvalidPathException exception) {
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
        } else {
            return property.value;
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
        if (parts.length == 2) {
            return getPropertyHolder(parts[0]).getProperty(parts[1]);
        } else if (parts.length == 3) {
            return getPropertyHolder(parts[1]).getProperty(parts[2]);
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
        return propertyHolders.get(name);
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
    }


    static class Property {
        private final String name;
        private String value;

        private DocumentContext asJson;

        private Property(String name) {
            this.name = name;
        }

        DocumentContext asJsonDocument() {
            if (asJson == null) {
                asJson = JsonPath.parse(value);
            }
            return asJson;
        }

        public void setValue(String value) {
            this.value = value;
            asJson = null;
        }

        public String getValue() {
            if (value == null) {
                if (asJson != null) {
                    value = asJson.jsonString();
                }
            }
            return value;
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
            properties.computeIfAbsent(name, Property::new).setValue(value);
        }

        public Property getProperty(String name) {
            return properties.get(name);
        }

        public Property getOrCreateProperty(String name) {
            return properties.computeIfAbsent(name, this::createProperty);
        }


        public Property createProperty(String name) {
            Property property = new Property(name);
            properties.put(name, property);
            return property;
        }

        public String getName() {
            return name;
        }

    }

    public static class StepContext extends PropertyHolder {
        private final RunnerContext runnerContext;
        private final Map<String, String> properties = new HashMap<>();


        StepContext(RunnerContext runnerContext, String name) {
            super(name);
            this.runnerContext = runnerContext;
        }

        String expand(Property property) {
            if (property == null) {
                return null;
            }
            return runnerContext.expand(property.getValue());
        }


        RunnerContext getRunnerContext() {
            return runnerContext;
        }
    }

    public static class CompositePropertyHolder extends PropertyHolder {
        private List<PropertyHolder> delegates = new ArrayList<>();

        public CompositePropertyHolder(String name) {
            super(name);
        }

        @Override
        public Property getProperty(String name) {
            for (PropertyHolder delegate : delegates) {
                Property property = delegate.getProperty(name);
                if (property != null) {
                    return property;
                }
            }
            return null;
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

        PropertiesContext(RunnerContext runnerContext, String name) {
            super(runnerContext, name);
        }
    }


    public static class RestRequestContext extends StepContext {
        static final String URL = "url";
        static final String REQUEST = "request";
        static final String RESPONSE = "Response";


        RestRequestContext(RunnerContext runnerContext, String name) {
            super(runnerContext, name);
        }

        @SuppressWarnings("WeakerAccess") // Used from karate
        public String url() {
            return expand(getProperty(URL));
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
            }
            throw new IllegalArgumentException("Unsupported type for request: " + response.getClass().getName());
        }

        public RestRequestContext response(String response) {
            setProperty(RESPONSE, response);
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
    }

    public static class ScriptContext extends StepContext {
        private static final String SCRIPT = "script";
        private static final String RESULT = "result";

        ScriptContext(RunnerContext runnerContext, String name) {
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
            return script(RunnerContext.toString(reader));
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

}




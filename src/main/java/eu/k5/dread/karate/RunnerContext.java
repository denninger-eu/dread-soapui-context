package eu.k5.dread.karate;

import com.eviware.soapui.SoapUI;
import com.jayway.jsonpath.*;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RunnerContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunnerContext.class);
    private static final PropertyHolder EMPTY = new PropertyHolder();
    private static final String GLOBAL = "Global";
    private static final String PROJECT = "Project";
    private static final String TEST_SUITE = "TestSuite";
    private static final String TEST_CASE = "TestCase";
    private final Map<String, PropertyHolder> propertyHolders = new HashMap<>();

    private ScriptEngineManager scriptEngineManager;
    private ScriptEngine scriptEngine;

    public RunnerContext() {
        propertyHolders.put(GLOBAL, SoapUI.getGlobalProperties());
        propertyHolders.put(PROJECT, new PropertyHolder());
        propertyHolders.put(TEST_SUITE, new PropertyHolder());
        propertyHolders.put(TEST_CASE, new PropertyHolder());
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
            TestCaseWrapper testCase = new TestCaseWrapper(testSuite, propertyHolders.get(TEST_CASE));
            scriptEngine.put("testRunner", new TestRunner(this, testCase));

        }
        return scriptEngine;
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

        TestCaseWrapper(TestSuiteWrapper testSuite, PropertyHolder propertyHolder) {
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


    public RequestContext requestStep(String name) {
        RequestContext request = new RequestContext(this, name);
        propertyHolders.put(name, request);
        return request;
    }

    public PropertyHolder propertiesStep(String name) {
        PropertiesContext properties = new PropertiesContext(this, name);
        propertyHolders.put(name, properties);
        return properties;
    }

    public Transfer transfer(String sourceProperty) {
        return new Transfer(this, new Query(sourceProperty, "", ""));
    }

    public Transfer transfer(String sourceProperty, String sourceExpression, String sourceLanguage) {
        return new Transfer(this, new Query(sourceProperty, sourceExpression, sourceLanguage));
    }

    public void setProperty(String property, String value) {
        updateOrCreateProperty(property, value);
    }

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

        public void to(String targetProperty) {
            to(targetProperty, "", "");
        }

        public void to(String targetProperty, String targetExpression, String targetLanguage) {
            String value = context.resolveValue(source);
            LOGGER.info("Source {} resolved to {}", source, value);
            context.updateValue(new Query(targetProperty, targetExpression, targetLanguage), value);
        }
    }


    private String resolveValue(Query query) {
        Property property = resolveProperty(query.property);
        if (property == null || property.value == null) {
            return null;
        }
        if (query.expression == null || query.expression.isEmpty()) {
            return property.value;
        }

        switch (query.language) {
            case "JSONPATH":
                return extractJsonPath(property.value, query.expression);
            default:
                throw new UnsupportedOperationException("Language not supported " + query.language);
        }
    }

    private void updateValue(Query target, String value) {
        if (target.getExpression() == null || target.getExpression().isEmpty()) {
            updateOrCreateProperty(target.getProperty(), value);
            return;
        }
        throw new UnsupportedOperationException();
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

    private static final Pattern EXPRESSION = Pattern.compile("\\$\\{(?<expression>\\=?[-._#a-zA-Z0-9\"]*?)}");

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

        public Query(String property, String expression, String language) {
            this.property = property;
            this.expression = expression;
            this.language = language;
        }

        public String getProperty() {
            return property;
        }

        public String getExpression() {
            return expression;
        }

        public String getLanguage() {
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

        public DocumentContext asJsonDocument() {
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
            return value;
        }


    }

    public static class PropertyHolder {
        private final Map<String, Property> properties = new HashMap<>();


        public void setProperty(String name, String value) {
            properties.computeIfAbsent(name, Property::new).setValue(value);
        }

        public Property getProperty(String name) {
            return properties.get(name);
        }

        public Property createProperty(String name) {
            Property property = new Property(name);
            properties.put(name, property);
            return property;
        }
    }

    public static class StepContext extends PropertyHolder {
        private final RunnerContext runnerContext;
        private final Map<String, String> properties = new HashMap<>();
        private final String name;


        StepContext(RunnerContext runnerContext, String name) {
            this.runnerContext = runnerContext;
            this.name = name;
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

    public static class PropertiesContext extends StepContext {

        PropertiesContext(RunnerContext runnerContext, String name) {
            super(runnerContext, name);
        }
    }


    public static class RequestContext extends StepContext {
        static final String URL = "url";
        static final String REQUEST = "request";
        static final String RESPONSE = "Response";


        RequestContext(RunnerContext runnerContext, String name) {
            super(runnerContext, name);
        }

        public String url() {
            return expand(getProperty(URL));
        }

        public RequestContext url(String newUrl) {
            setProperty(URL, newUrl);
            return this;
        }

        public String request() {
            return expand(getProperty(REQUEST));
        }

        public RequestContext request(String request) {
            setProperty(REQUEST, request);
            return this;
        }

        public RequestContext request(Object value) {
            if (value == null) {
                return this;
            } else if (value instanceof Map) {
                JSONObject jsonObject = new JSONObject((Map) value);
                return request(jsonObject.toJSONString());
            }

            throw new IllegalArgumentException("Unsupported type for request: " + value.getClass().getName());
        }

        public String response() {
            return expand(getProperty(RESPONSE));
        }

        public RequestContext response(Object response) {
            if (response == null) {
                return this;
            } else if (response instanceof Map) {
                JSONObject jsonObject = new JSONObject((Map) response);
                return response(jsonObject.toJSONString());
            }
            throw new IllegalArgumentException("Unsupported type for request: " + response.getClass().getName());
        }

        public RequestContext response(String response) {
            setProperty(RESPONSE, response);
            return this;
        }

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

        public ScriptContext script(Reader reader) {
            return script(RunnerContext.toString(reader));
        }

        public ScriptContext script(String script) {
            setProperty(SCRIPT, script);
            return this;
        }

        public void result(String result) {
            setProperty(RESULT, result);
        }

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

    private static final String toString(Reader reader) {
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




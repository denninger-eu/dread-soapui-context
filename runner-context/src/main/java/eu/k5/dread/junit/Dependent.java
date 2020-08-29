package eu.k5.dread.junit;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.MethodOrdererContext;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class Dependent {

    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface DependsOn {
        String[] value();
    }


    public static class DependsOnMethodOrder implements MethodOrderer {
        private static final Logger logger = LoggerFactory.getLogger(Random.class);
        // default value = 0 to make independent test run first, give it MAX_INT to make it run last
        // Consider making this public, but not see any serious need yet
        private static final int INDEPENDENT_TEST_PRIORITY = 0;

        @Override
        public void orderMethods(MethodOrdererContext context) {
            // Directed Acyclic Graph (DAG) to represent order relationship between methods
            // An edge from A -> B means method A should run after B
            Map<String, String[]> digraph = context.getMethodDescriptors().stream().filter(
                    descriptor -> descriptor.isAnnotated(DependsOn.class)).collect(
                    Collectors.toMap(descriptor -> descriptor.getMethod().getName(),
                            descriptor -> descriptor.findAnnotation(DependsOn.class).map(DependsOn::value).get()));

            // Give each an @Order's value equivalent to its number of previous dependencies
            Map<String, Integer> dependencySize = new HashMap<>();

            try {
                // run depth first search through all vertexes (methods) in graph to find dependencies
                digraph.keySet().forEach(name -> depthFirstSearch(name, digraph, dependencySize));
            } catch (IllegalArgumentException exception) {
                // cannot throw an exception here since MethodOrderer is not supposed to throw exception
                logger.error("ERROR - Some arguments from @DependsOn annotations form cyclic dependencies, which would cause undefined behavior!", exception);
            }

            context.getMethodDescriptors().sort(
                    Comparator.comparing(descriptor -> dependencySize.getOrDefault(descriptor.getMethod().getName(),
                            INDEPENDENT_TEST_PRIORITY)));
        }

        @Override
        public Optional<ExecutionMode> getDefaultExecutionMode() {
            return Optional.empty();
        }

        /**
         * This method will compute the (number of methods that must be run before the annotated method) + 1 (+1 to simplify computation)
         */
        private int depthFirstSearch(String name, Map<String, String[]> digraph, Map<String, Integer> dependencySize) {
            if (dependencySize.containsKey(name)) {
                return dependencySize.get(name);
            }
            // mark entering this node
            dependencySize.put(name, -1);

            String[] ancestors = digraph.get(name);
            int total = 1;

            if (ancestors != null) {
                for (String ancestor : ancestors) {
                    int sz = depthFirstSearch(ancestor, digraph, dependencySize);
                    // cycle detected, -1 means in process but not yet finished -> should not appear
                    if (sz == -1) {
                        throw new IllegalArgumentException(
                                String.format("Cycle detected between %s and %s", name, ancestor));
                    }
                    total += sz;

                }
            }

            // update with correct value
            dependencySize.put(name, total);
            return total;


        }
    }

    public static class DependsOnTestWatcher implements ExecutionCondition, TestWatcher {
        /**
         * successfulTests stores tests that are successfully executed
         */
        private Set<String> successfulTests = new HashSet<>();

        @Override
        public void testDisabled(ExtensionContext context, Optional<String> reason) {
        }

        @Override
        public void testSuccessful(ExtensionContext context) {
            context.getTestMethod().ifPresent(method -> successfulTests.add(method.getName()));
        }

        @Override
        public void testAborted(ExtensionContext context, Throwable cause) {
        }

        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
        }

        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            Method method = context.getTestMethod().orElse(null);


            if (method != null) {
                DependsOn annotation = method.getAnnotation(DependsOn.class);
                if (annotation != null) {
                    // loop through all dependent methods' name
                    Optional<String> unsuccessfulMethod = Arrays.stream(annotation.value()).filter(
                            name -> !successfulTests.contains(name)).findAny();

                    if (unsuccessfulMethod.isPresent()) {
                        // disable this test
                        return ConditionEvaluationResult.disabled(String.format(
                                "'%s()' cannot be executed because its dependent test '%s()' either failed or just did not execute!",
                                method.getName(), unsuccessfulMethod.get()));
                    }
                }
            }

            return ConditionEvaluationResult.enabled("Enable by Default");
        }
    }
}

package eu.k5.dread.junit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(Dependent.DependsOnTestWatcher.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(Dependent.DependsOnMethodOrder.class)
class ExampleDependsOnAnnotationTestCase {
    private int counter = 0;

    @AfterEach
    void increaseCounter() {
        counter += 1;
    }

    @Test
    void alpha() {
        assertEquals(1, counter);
    }



    @Test
    @Dependent.DependsOn("beta")
    void gamma() {
        assertEquals(3, counter);
    }

    @Test
    @Dependent.DependsOn("gamma")
    void delta() {
        assertEquals(4, counter);
    }
    @Test
    @Dependent.DependsOn("alpha")
    void beta() {
        assertEquals(2, counter);
    }
    @Test
    void independentTest() {
        assertEquals(0, counter, "Independent tests should run first");
    }
}
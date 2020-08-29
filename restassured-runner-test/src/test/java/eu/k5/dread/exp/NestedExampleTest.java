package eu.k5.dread.exp;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class NestedExampleTest {


    @Test
    public void test1() {
        System.out.println("test1");
    }

    @Nested
    public class NestedEx {


        @Test
        public void test2() {
            System.out.println("test2");
        }

    }
}

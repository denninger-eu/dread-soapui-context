package eu.k5.dread.karate.manual;

import com.intuit.karate.junit5.Karate;

public class ManualRunner {


    @Karate.Test
    public Karate generated() {
        return new Karate().feature("classpath:generated/restrunnable/main.feature");
    }

    @Karate.Test
    public Karate manual() {
        return new Karate().feature("classpath:manual/minimal.feature");
    }
}

package eu.k5.dread.soapui.karate;

import com.intuit.karate.junit5.Karate;

public class ManualRunner {


    @Karate.Test
    public Karate generated() {
        return new Karate().feature("classpath:restrunnable/case.feature");
    }

    @Karate.Test
    public Karate manual() {
        return new Karate().feature("classpath:manual/minimal.feature", "classpath:restrunnable/main.feature");
    }

    @Karate.Test
    public Karate restrunnable() {
        return new Karate().feature("classpath:restrunnable/main.feature");
    }
}

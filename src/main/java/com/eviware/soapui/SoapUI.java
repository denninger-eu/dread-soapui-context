package com.eviware.soapui;

import eu.k5.dread.karate.RunnerContext;

public class SoapUI {
    private static final SoapUI INSTANCE = new SoapUI();
    private RunnerContext.PropertyHolder propertyHolder = new RunnerContext.PropertyHolder();

    public static SoapUI getInstance() {
        return INSTANCE;
    }

    public static RunnerContext.PropertyHolder getGlobalProperties() {
        return getInstance().propertyHolder;
    }
}

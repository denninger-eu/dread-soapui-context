// Get a test case property
def testCaseProperty = testRunner.testCase.getPropertyValue("MyProp")
// Get a test suite property
def testSuiteProperty = testRunner.testCase.testSuite.getPropertyValue( "MyProp" )
// Get a project property
def projectProperty = testRunner.testCase.testSuite.project.getPropertyValue( "MyProp" )
// Get a global property
def globalProperty = com.eviware.soapui.SoapUI.globalProperties.getPropertyValue( "MyProp" )

def someValue = "value"


// Set a test case property
testRunner.testCase.setPropertyValue( "MyProp", someValue )
// Set a test suite property
testRunner.testCase.testSuite.setPropertyValue( "MyProp", someValue )
// Set a project property
testRunner.testCase.testSuite.project.setPropertyValue( "MyProp", someValue )
// Set a global property
com.eviware.soapui.SoapUI.globalProperties.setPropertyValue( "MyProp", someValue )


import groovy.json.*; 
def jsonSlurper = new JsonSlurper();
def json = testRunner.testCase.testSteps['updateResource'].httpRequest.requestContent;
def object = jsonSlurper.parseText(json);
object.date = "date";
testRunner.testCase.testSteps['updateWithDate'].httpRequest.requestContent = new JsonBuilder(object).toPrettyString();

log.info(testRunner.testCase.testSteps['updateWithDate'].testRequest.class.name)

log.info(testRunner.testCase.testSteps['updateResource'].class.name)

log.info(log.getClass())
log.info("test");
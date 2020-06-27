Feature: Manually created feature

Scenario: minimal scenario
  * def RunnerContext = Java.type('eu.k5.dread.karate.RunnerContext');
  * def ctx = new RunnerContext()

  * def prop = ctx.propertiesStep("prop")
  * print prop.setProperty("key", "value")


  * def createResourceStep = ctx.requestStep("createResource").url( "http://localhost:8080/resource" ).request(  read("soapui-manual-createResourceStep.json"))
  * def getResourceStep = ctx.requestStep("getResource").url("http://localhost:8080/resource/${#Project#projectProperty}")
  * def updateResourceStep = ctx.requestStep("updateResource").url("http://localhost:8080/resource/${#Project#projectProperty}").request("{ \"id\":\"${#Project#projectProperty}\", \"value\": \"updated\" }")

  Given url createResourceStep.url()
  And request createResourceStep.request()
  And header Accept = 'application/json'
  And header Content-Type = 'application/json'
  When method POST
  Then def temp = createResourceStep.response(response)
  And status 200
  # And match createResourceStep.assertJsonExists("$.id") == true

  * def t = ""
  * set t = ctx.transfer("#createResource#Response", "$.id", "JSONPATH").to(""#Project#projectProperty")
  * set t = ctx.transfer("#createResource#Response", "$.id", "JSONPATH").to("#Project#projectProperty")

  Given url getResourceStep.url()
  And header Accept = 'application/json'
  And header Content-Type = 'application/json'
  When method GET
  Then def t3 = getResourceStep.response(response)

  Given url updateResourceStep.url()
  And request updateResourceStep.request()
  And header Accept = 'application/json'
  And header Content-Type = 'application/json'
  When method PUT
  Then def temp = updateResourceStep.response(response)

  * def t6 = ctx.groovy( read( "groovy.groovy" ) )






Feature: Manually created feature

Background:
  * def RunnerContext = Java.type('eu.k5.dread.soapui.SoapuiContext');
  * def ctx = new RunnerContext()
  * print ctx.setProperty("#TestCase#baseUrl","http://localhost:8080/crud")

Scenario: minimal scenario

	
	* def Properties = ctx.propertiesStep("Properties1")
	* print Properties.setProperty("key","keyValue")
	* print Properties.setProperty("date","${=java.time.LocalDateTime.now()}")
	* print Properties.setProperty("dynamicScript","\"test\"")
	* print Properties.setProperty("updatedValue","updated")
	* def createResourceRequest = read("createResourceRequest.txt")
	* def createResource = ctx.requestStep("createResource").url("${#TestCase#baseUrl}/resources").request(createResourceRequest)
	* def getResource = ctx.requestStep("getResource").url("${#TestCase#baseUrl}/resources/${#Project#projectProperty}")
	* def updateResourceRequest = read("updateResourceRequest.txt")
	* def updateResource = ctx.requestStep("updateResource").url("${#TestCase#baseUrl}/resources/${#Project#projectProperty}").request(updateResourceRequest)
	# Script Groovy
	* def GroovyScript = read("GroovyScript.groovy")
	* def Groovy = ctx.groovyScript("Groovy").script(GroovyScript)
	
	# Script Groovy2
	* def Groovy2Script = read("Groovy2Script.groovy")
	* def Groovy2 = ctx.groovyScript("Groovy2").script(Groovy2Script)
	
	
	* def Properties2 = ctx.propertiesStep("Properties2")
	* print Properties2.setProperty("key","keyValue")
	* print Properties2.setProperty("date","${=java.time.LocalDateTime.now()}")
	* print Properties2.setProperty("dynamicScript","\"test\"")
	* print Properties2.setProperty("updatedValue","updated")
	# createResource
	Given url createResource.url()
	  And request createResource.request()
	  And param queryParam = "paramV"
	  And header headerP = "headerV"
	  And header Accept = "application/json"
	  And header Content-Type = "application/json"
	When  method POST
	Then  print createResource.response(response)
	  And status 200
	  And match createResource.assertJsonExists("$.id") == true
	
	# transfer
	* print ctx.transfer("#createResource#Response","$.id","JSONPATH").to("#Project#projectProperty")
	* print ctx.transfer("#createResource#Response","$.id","JSONPATH").to("#TestSuite#suiteProperty")
	* print ctx.transfer("#createResource#Response","$.id","JSONPATH").to("#TestCase#caseProperty")
	
	# getResource
	Given url getResource.url()
	  And param queryParam = ""
	  And header Accept = "application/json"
	When  method GET
	Then  print getResource.response(response)
	  And match getResource.assertJsonExists("$.id") == true
	
	# Delay
	* print ctx.sleep(1000)
	
	# transferBody
	* print ctx.transfer("#getResource#Response").to("#updateResource#Request")
	* print ctx.transfer("#Properties#updatedValue").to("#updateResource#Request","$.payload","JSONPATH")
	
	# updateResource
	Given url updateResource.url()
	  And request updateResource.request()
	  And param queryParam = ""
	  And header Accept = "application/json"
	  And header Content-Type = "application/json"
	When  method PUT
	Then  print updateResource.response(response)
	  And match updateResource.assertJsonExists("$.id") == true
	
	# Script Groovy
	* def t2 = Groovy.execute()
	
	# Script Groovy2
	* def t3 = Groovy2.execute()
	

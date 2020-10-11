Feature: case

Background:
	# background

Scenario: case
	* def Context = Java.type('eu.k5.dread.soapui.SoapuiContext')
	* def ctx = new Context()

	* def Properties = ctx.propertiesStep("Properties")
	* if (true) Properties.setProperty("key","keyValue")
	* if (true) Properties.setProperty("date","${=java.time.LocalDateTime.now()}")
	* if (true) Properties.setProperty("dynamicScript","\"test\"")
	* if (true) Properties.setProperty("payload","updated")
	* def createResourceRequest = read("createResourceRequest.txt")
	* def createResource = ctx.requestStep("createResource").url("${#TestCase#baseUrl}/resources").request(createResourceRequest)
	* def getResource = ctx.requestStep("getResource").url("${#TestCase#baseUrl}/resources/${template}").property("template","${#Project#projectProperty}")
	* def updateResourceRequest = read("updateResourceRequest.txt")
	* def updateResource = ctx.requestStep("updateResource").url("${#TestCase#baseUrl}/resources/${template}").request(updateResourceRequest).property("template","${#Project#projectProperty}")
	# Script Groovy
	* def GroovyScript = read("GroovyScript.groovy")
	* def Groovy = ctx.groovyScript("Groovy").script(GroovyScript)

	* def updateWithDateRequest = read("updateWithDateRequest.txt")
	* def updateWithDate = ctx.requestStep("updateWithDate").url("${#TestCase#baseUrl}/resources/${template}").request(updateWithDateRequest).property("template","${#Project#projectProperty}")
	# postInit start
	* if (true) ctx.setProperty("#TestCase#baseUrl","http://localhost:8080/crud")
	# postInit end


	# createResource
	# POST /resources
	Given url createResource.url()
	  And request createResource.request()
	  And param queryParam = "paramV"
	  And header headerP = "headerV"
	  And header Accept = "application/json"
	  And header Content-Type = "application/json"
	When  method POST
	Then  if (true) createResource.response(response).status(responseStatus)
	  And status 200
	  And if (true) createResource.assertJsonPathExists("$.id","true")

	# transfer
	* if (true) ctx.transfer("#createResource#Response","$.id","JSONPATH").to("#Project#projectProperty")
	* if (true) ctx.transfer("#createResource#Response","$.id","JSONPATH").to("#TestSuite#suiteProperty")
	* if (true) ctx.transfer("#createResource#Response","$.id","JSONPATH").to("#TestCase#caseProperty")

	# getResource
	# GET /resources/${template}
	Given url getResource.url()
	  And header Accept = "application/json"
	When  method GET
	Then  if (true) getResource.response(response).status(responseStatus)
	  And if (true) getResource.assertJsonPathExists("$.id","true")

	# Delay
	* if (true) ctx.sleep(1000)

	# transferResource
	* if (true) ctx.transfer("#getResource#Response").to("#updateResource#Request")
	* if (true) ctx.transfer("#Properties#payload").to("#updateResource#Request","$.payload","JSONPATH")
	* if (true) ctx.transfer("#Properties#date").to("#updateResource#Request","$.date","JSONPATH")

	# updateResource
	# PUT /resources/${template}
	Given url updateResource.url()
	  And request updateResource.request()
	  And header Accept = "application/json"
	  And header Content-Type = "application/json"
	When  method PUT
	Then  if (true) updateResource.response(response).status(responseStatus)
	  And if (true) updateResource.assertJsonPathExists("$.id","true")

	# Script Groovy
	* def t1 = Groovy.execute()
	* print t1

	# updateWithDate
	# PUT /resources/${template}
	Given url updateWithDate.url()
	  And request updateWithDate.request()
	When  method PUT
	Then  if (true) updateWithDate.response(response).status(responseStatus)

	# post start
	# postexecute
	# post end

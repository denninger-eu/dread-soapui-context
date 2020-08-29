package eu.k5.dread.karate.test.rest

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.lang.IllegalArgumentException
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@RestController
@RequestMapping("/crud01")
open class TestRestController() {

    @GetMapping("/resources")
    fun getResources(): List<TestResource> {
        return ArrayList(repository.values)
    }

    @RequestMapping(value = ["/resources"], method = [RequestMethod.POST])
    fun postResource(@RequestBody resource: TestResource): TestResource {
        LOGGER.info("Received payload: {}", resource.payload)

        val id = UUID.randomUUID().toString()
        resource.id = id
        resource.date = Instant.now().toString()
        resource.age = ""
        repository[id] = resource
        return resource
    }

    @RequestMapping(value = ["/resources/{id}"], method = [RequestMethod.PUT])
    fun putResource(@PathVariable("id") id: String, @RequestBody resource: TestResource): TestResource {
        LOGGER.info("Updated Resource {} {}", id, resource)
        val existing = repository[id] ?: throw IllegalArgumentException("Resource missing with id:$id")
        existing.date = Instant.now().toString()
        repository[id] = resource
        return resource
    }

    @RequestMapping(value = ["/resources/{id}"], method = [RequestMethod.GET])
    fun getResource(@PathVariable("id") id: String): TestResource {
        val testResource = repository[id] ?: throw IllegalArgumentException("Resource missing with id:$id")
        testResource.age = Duration.between(Instant.parse(testResource.date!!), Instant.now()).seconds.toString()
        return testResource!!
    }

    companion object {
        val repository = ConcurrentHashMap<String, TestResource>()
        private val LOGGER = LoggerFactory.getLogger(TestRestController::class.java)

        init {
            val testResource = TestResource()
            testResource.id = "example"
            testResource.payload = "payload"
            repository["example"] = testResource
        }
    }
}


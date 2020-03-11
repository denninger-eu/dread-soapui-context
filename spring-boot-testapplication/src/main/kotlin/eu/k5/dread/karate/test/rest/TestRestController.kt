package eu.k5.dread.karate.test.rest

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@RestController
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
        repository[id] = resource
        return resource
    }

    @RequestMapping(value = ["/resources/{id}"], method = [RequestMethod.PUT])
    fun putResource(@PathVariable("id") id: String, @RequestBody resource: TestResource): TestResource {
        LOGGER.info("Updated Resource {} {}", id, resource)
        repository[id] = resource
        return resource
    }

    @RequestMapping(value = ["/resources/{id}"], method = [RequestMethod.GET])
    fun getResource(@PathVariable("id") id: String): TestResource {
        return repository[id]!!
    }

    companion object {
        val repository = ConcurrentHashMap<String, TestResource>()
        private val LOGGER = LoggerFactory.getLogger(TestRestController::class.java)
    }
}

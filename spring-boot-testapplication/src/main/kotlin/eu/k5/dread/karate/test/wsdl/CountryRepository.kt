package eu.k5.dread.karate.test.wsdl

import eu.k5.dread.karate.test.wsdl.model.Country
import org.springframework.stereotype.Component
import java.lang.IllegalArgumentException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

@Component
class CountryRepository {
    private val countries = ConcurrentHashMap<String?, Country>()

    fun findCountry(name: String?): Country? = countries.values.firstOrNull { it.name == name }

    fun createCountry(country: Country?): Country? {
        requireNotNull(country?.name) { "No country with name given" }
        require(findCountry(country?.name) == null) { "Country already exists" }

        country!!.id = UUID.randomUUID().toString()
        countries[country.id] = country
        return country
    }

    @PostConstruct
    fun init() {
        val country = Country()
        country.name = "Spain"
        country.population = 100
        country.capital = "Madrid"
        createCountry(country)
    }

    fun getCountries() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getCountry(id: String?): Country? = countries[id]

}
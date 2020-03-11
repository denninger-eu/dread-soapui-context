package eu.k5.dread.karate.test.wsdl

import eu.k5.dread.karate.test.wsdl.model.Country
import org.springframework.stereotype.Component
import java.lang.IllegalArgumentException
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

@Component
class CountryRepository {
    private val countries = ConcurrentHashMap<String?, Country>()

    fun findCountry(name: String): Country? = countries[name]

    fun createCountry(country: Country?): Country? {
        requireNotNull(country?.name) { "No country with name" }
        require(!countries.contains(country?.name)) { "Country already exists" }
        countries[country!!.name] = country
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

}
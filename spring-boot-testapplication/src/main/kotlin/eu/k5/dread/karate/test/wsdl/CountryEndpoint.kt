package eu.k5.dread.karate.test.wsdl

import eu.k5.dread.karate.test.wsdl.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.ws.server.endpoint.annotation.Endpoint
import org.springframework.ws.server.endpoint.annotation.PayloadRoot
import org.springframework.ws.server.endpoint.annotation.RequestPayload
import org.springframework.ws.server.endpoint.annotation.ResponsePayload


@Endpoint
open class CountryEndpoint @Autowired
constructor(private val countryRepository: CountryRepository) {

    @PayloadRoot(namespace = Model.Companion.NAMESPACE, localPart = "getCountryRequest")
    @ResponsePayload
    open fun getCountry(@RequestPayload request: GetCountryRequest): GetCountryResponse {
        val response = GetCountryResponse()
        response.country = countryRepository.getCountry(request.id ?: "")
        return response
    }

    @PayloadRoot(namespace = Model.Companion.NAMESPACE, localPart = "findCountryRequest")
    @ResponsePayload
    open fun findCountry(@RequestPayload request: FindCountryRequest): FindCountryResponse {
        val response = FindCountryResponse()
        response.country = countryRepository.findCountry(request.name ?: "")
        return response
    }

    @PayloadRoot(namespace = Model.Companion.NAMESPACE, localPart = "getCountriesRequest")
    @ResponsePayload
    open fun getCountries(@RequestPayload request: GetCountriesRequest): GetCountriesResponse {
        val response = GetCountriesResponse()
        countryRepository.getCountries()
        return response
    }

    @PayloadRoot(namespace = Model.Companion.NAMESPACE, localPart = "createCountryRequest")
    @ResponsePayload
    open fun createCountry(@RequestPayload request: CreateCountryRequest): CreateCountryResponse {
        val response = CreateCountryResponse()
        response.country = countryRepository.createCountry(request.country)
        return response
    }

    @PayloadRoot(namespace = Model.Companion.NAMESPACE, localPart = "updateCountryRequest")
    @ResponsePayload
    open fun updateCountry(@RequestPayload request: UpdateCountryRequest): UpdateCountryResponse {
        val response = UpdateCountryResponse()
        //response.country = countryRepository.createCountry(request.country)
        return response
    }

}
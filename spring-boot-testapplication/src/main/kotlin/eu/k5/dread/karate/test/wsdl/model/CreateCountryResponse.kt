package eu.k5.dread.karate.test.wsdl.model

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement(name = "createCountryResponse", namespace = Model.NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
class CreateCountryResponse {

    @XmlElement(name = "country", namespace = Model.NAMESPACE)
    var country: Country? = null
}
package eu.k5.dread.karate.test.wsdl.model

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement(name = "country", namespace = Model.NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
class Country {
    @XmlElement
    var name: String? = null
    @XmlElement
    var population: Int? = null
    @XmlElement
    var capital: String? = null

}
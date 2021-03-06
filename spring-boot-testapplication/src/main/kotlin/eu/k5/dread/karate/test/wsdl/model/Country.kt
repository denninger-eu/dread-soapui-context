package eu.k5.dread.karate.test.wsdl.model

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement(name = "country", namespace = Model.NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
class Country {

    @XmlElement(name = "id", namespace = Model.NAMESPACE)
    var id: String? = null
    @XmlElement(name = "name", namespace = Model.NAMESPACE)
    var name: String? = null
    @XmlElement(name = "population", namespace = Model.NAMESPACE)
    var population: Int? = null
    @XmlElement(name = "capital", namespace = Model.NAMESPACE)
    var capital: String? = null

}
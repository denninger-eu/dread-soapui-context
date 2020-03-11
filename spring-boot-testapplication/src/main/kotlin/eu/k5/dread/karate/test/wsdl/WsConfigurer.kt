package eu.k5.dread.karate.test.wsdl

import eu.k5.dread.karate.test.wsdl.model.Model
import org.springframework.core.io.ClassPathResource
import org.springframework.xml.xsd.SimpleXsdSchema
import org.springframework.xml.xsd.XsdSchema
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.ws.config.annotation.EnableWs
import org.springframework.ws.config.annotation.WsConfigurerAdapter
import org.springframework.ws.transport.http.MessageDispatcherServlet

@EnableWs
@Configuration
open class WsConfigurer : WsConfigurerAdapter() {

    @Bean
    open fun messageDispatcherServlet(applicationContext: ApplicationContext): ServletRegistrationBean<*> {
        val servlet = MessageDispatcherServlet()
        servlet.setApplicationContext(applicationContext)
        servlet.isTransformWsdlLocations = true
        return ServletRegistrationBean(servlet, "/ws/*")
    }

    @Bean(name = ["countries"])
    open fun defaultWsdl11Definition(countriesSchema: XsdSchema): DefaultWsdl11Definition {
        val wsdl11Definition = DefaultWsdl11Definition()
        wsdl11Definition.setPortTypeName("CountriesPort")
        wsdl11Definition.setLocationUri("/ws")
        wsdl11Definition.setTargetNamespace(Model.NAMESPACE)
        wsdl11Definition.setSchema(countriesSchema)
        return wsdl11Definition
    }

    @Bean
    open fun countriesSchema(): XsdSchema {
        return SimpleXsdSchema(ClassPathResource("countries.xsd"))
    }
}
// NO_XML_CONFIG
// REF: java
// REF: java.java

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackages = arrayOf("<caret>java"))
open class App {

}
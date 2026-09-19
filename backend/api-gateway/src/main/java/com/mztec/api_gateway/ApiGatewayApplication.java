package com.mztec.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Repare que aqui NÃO precisamos de nenhuma anotação especial tipo @EnableEurekaClient.
// A partir do momento que a dependência spring-cloud-starter-netflix-eureka-client
// está no classpath (veja o pom.xml), o Spring Boot ativa tudo automaticamente
// via autoconfiguração. O mesmo vale para o Gateway.
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

}

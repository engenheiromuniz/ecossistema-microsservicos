package com.mztec.eureka_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

// @EnableEurekaServer é o que realmente liga o "modo servidor" do Eureka.
// Sem essa anotação, a aplicação sobe normalmente, mas nenhuma das rotas
// do Eureka (dashboard, /eureka/apps, etc.) fica disponível — foi exatamente
// esse detalhe que nos deu dor de cabeça durante o desenvolvimento original!
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }

}

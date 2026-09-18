package com.mztec.ms_matricula;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients // ← liga o mecanismo que escaneia e cria as implementações dos @FeignClient
public class MsMatriculaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsMatriculaApplication.class, args);
    }
}
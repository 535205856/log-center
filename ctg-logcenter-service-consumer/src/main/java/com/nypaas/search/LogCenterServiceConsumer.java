package com.nypaas.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;

@SpringBootApplication
public class LogCenterServiceConsumer extends SpringBootServletInitializer {

  public static void main(String[] args) throws Exception {
    SpringApplication.run(LogCenterServiceConsumer.class, args);
  }

}
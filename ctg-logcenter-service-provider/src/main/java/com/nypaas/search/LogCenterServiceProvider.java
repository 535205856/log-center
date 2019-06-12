package com.nypaas.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;

@SpringBootApplication
//@ComponentScan(basePackages = {"com.nypaas.search","org.elasticsearch"})
public class LogCenterServiceProvider extends SpringBootServletInitializer {
  public static void main(String[] args) {
    SpringApplication.run(LogCenterServiceProvider.class, args);
  }

}
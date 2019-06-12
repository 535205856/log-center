package com.nypass.search;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;

@SpringBootApplication
@MapperScan("com.nypass.search.mapper")
public class DBProvideApp extends SpringBootServletInitializer {
  public static void main(String[] args) {
    SpringApplication.run(DBProvideApp.class, args);
  }

}
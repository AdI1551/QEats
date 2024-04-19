
/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

@SpringBootApplication

public class QEatsApplication {

  public static void main(String[] args) {
    SpringApplication.run(QEatsApplication.class, args);
    final Logger log = LoggerFactory.getLogger(QEatsApplication.class);
    // TIP:MODULE_RESTAPI: If your server starts successfully,
    // you can find the following message in the logs.
    

    log.info("Congrats! Your QEatsApplication server has started");
 
  }

  
  @Bean // Want a new obj every time
  @Scope("prototype")
  public ModelMapper modelMapper() {
    return new ModelMapper();
  }

}

package com.main.controller;

import com.main.feign.TestFiegnClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/feign-client")
public class FeignClientController {

    @Autowired
    TestFiegnClient testFiegnClient;

    @GetMapping("/test")
    public String getFeignClientTest(){
        return testFiegnClient.fetchHelloFromDemo();
    }
}

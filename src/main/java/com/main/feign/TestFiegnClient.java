package com.main.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("STUDENT-SERVICE")
public interface TestFiegnClient {

    @GetMapping("/student/test-demo")
    public String fetchHelloFromDemo();
}

package com.main.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {
    
        @ResponseBody
        @GetMapping("/test")
        public String home(){
            System.out.println("Wellcome to Arjun Dhatbale.");
            return "Wellcome to Arjun Dhatbale.";
        }
}

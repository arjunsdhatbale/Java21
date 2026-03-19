package com.main.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {
    
        @ResponseBody
        @GetMapping("/test")
        public String home(){
            System.out.println("Welcome to Arjun Chatbase.");
            return "Welcome to Arjun Chatbase.";
        }
}

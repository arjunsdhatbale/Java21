package com.main.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class NotificationController {

    Logger logger = LoggerFactory.getLogger(NotificationController.class);

    // /app/send-message
    @MessageMapping("/send-message")
    @SendTo("/topic/notification")
    public String sendMessage(String message){
        logger.info("Request received to send notification.");
        return message;
    }
}

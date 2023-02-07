package com.example.momobe.jenkins_test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JenkinsController {
    @GetMapping("/jenkins")
    public String jenkins() {
        return "welcome jenkins";
    }
}

package main.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DefaultController {

    @GetMapping(value = "/api/status")
    private ResponseEntity<?> status() {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(value = "/")
    private ResponseEntity<?> get() {
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @RequestMapping(method = {RequestMethod.OPTIONS, RequestMethod.GET}, value = "/**")
    public ResponseEntity<?> redirectAllRequest() {
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }
}

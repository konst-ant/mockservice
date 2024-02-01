package com.mockservice.web.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/top-page")
public class TopAppController extends AbstractRestController {

    @PostMapping("find")
    public ResponseEntity<String> postEntity() {
        return mock();
    }
}

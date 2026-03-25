package com.picture.backend.controller;

import com.picture.backend.common.BaseResponse;
import com.picture.backend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class MainController {

    @GetMapping("/health")
    public BaseResponse<String> health() {
        log.info("health check");
        return ResultUtils.success("ok");
    }
}

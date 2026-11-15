package com.youkong.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 前端打包进 static 目录后，刷新这些路径需要回落到 index.html。
 */
@Controller
public class SpaController {

    @GetMapping({"/", "/staff", "/time", "/confirm", "/done/{id}", "/mine", "/admin", "/admin/**"})
    public String index() {
        return "forward:/index.html";
    }
}

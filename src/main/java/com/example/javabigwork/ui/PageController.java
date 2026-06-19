package com.example.javabigwork.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({
            "/",
            "/chat",
            "/vocabulary",
            "/review",
            "/writing",
            "/news",
            "/report",
            "/wordbank"
    })
    public String app() {
        return "forward:/index.html";
    }
}

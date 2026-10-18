package com.example.javabigwork.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({
            "/",
            "/chat",
            "/mission",
            "/vocabulary",
            "/review",
            "/writing",
            "/news",
            "/report",
            "/wordbank",
            "/login",
            "/register",
            "/admin"
    })
    public String app() {
        return "forward:/index.html";
    }
}

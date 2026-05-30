package evo.developers.com.cashcare.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping({"/", "/auth"})
    public String authPage() {
        return "auth";
    }

    @GetMapping("/init")
    public String initPage() {
        return "redirect:/";
    }

    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "dashboard";
    }
}

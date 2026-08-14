package com.enviro.assessment.junior.sive.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Serves the frontend as server-rendered Thymeleaf views.
 *
 * Each method just returns a view name, which Spring's ThymeleafViewResolver
 * resolves to src/main/resources/templates/{name}.html - the HTML/CSS/JS
 * itself is plain (no th:* attributes), Thymeleaf just needed a real
 * ViewResolver in place to stop the "circular view path" error that occurs
 * with the default InternalResourceViewResolver and no template engine.
 *
 * CSS/JS are served separately as static resources from
 * src/main/resources/static/{css,js}/... at /css/... and /js/..., which
 * Spring Boot wires up automatically - no mapping needed here for those.
 *
 * IMPORTANT: every page-to-page link/redirect in the HTML/JS (login.js,
 * auth.js, settings.js, and the <a href> tags) must match these exact
 * paths, since templates/*.html is not directly web-accessible - only
 * these mapped URLs render it.
 */
@Controller
public class InvestorViewController {

    /** GET / - convenience redirect so visiting the app root lands on the login page. */
    @GetMapping("/")
    public RedirectView root() {
        return new RedirectView("/enviro365/login");
    }

    @GetMapping("/enviro365/login")
    public String login() {
        return "login";
    }

    @GetMapping("/enviro365/investor/dashboard")
    public String investorDashboard() {
        return "index";
    }

    @GetMapping("/enviro365/admin")
    public String adminDashboard() {
        return "admin";
    }

    @GetMapping("/enviro365/settings")
    public String settings() {
        return "settings";
    }
}

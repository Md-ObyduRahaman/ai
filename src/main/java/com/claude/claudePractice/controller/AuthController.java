package com.claude.claudePractice.controller;

import com.claude.claudePractice.model.CartItemEntity;
import com.claude.claudePractice.model.User;
import com.claude.claudePractice.repository.CartItemRepository;
import com.claude.claudePractice.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Year;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CartItemRepository cartItemRepository;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          CartItemRepository cartItemRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.cartItemRepository = cartItemRepository;
    }

    private int getCartCount() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        if (username.equals("anonymousUser")) return 0;
        return cartItemRepository.findByUsername(username)
            .stream().mapToInt(CartItemEntity::getQuantity).sum();
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("pageTitle", "Login - ShopEasy");
        model.addAttribute("brandName", "ShopEasy");
        model.addAttribute("currentYear", Year.now().getValue());
        model.addAttribute("cartCount", getCartCount());
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("pageTitle", "Register - ShopEasy");
        model.addAttribute("brandName", "ShopEasy");
        model.addAttribute("currentYear", Year.now().getValue());
        model.addAttribute("cartCount", getCartCount());
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password, Model model) {
        if (userRepository.findByUsername(username).isPresent()) {
            model.addAttribute("error", "Username already exists");
            return "register";
        }
        String encoded = passwordEncoder.encode(password);
        userRepository.save(new User(username, encoded, "ROLE_USER"));
        return "redirect:/login";
    }
}

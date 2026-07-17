package com.claude.claudePractice.controller;

import com.claude.claudePractice.model.CartItemEntity;
import com.claude.claudePractice.model.Product;
import com.claude.claudePractice.repository.CartItemRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Year;

@Controller
public class HomeController {

    private final CartItemRepository cartItemRepository;

    public HomeController(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    private int getCartCount() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        if (username.equals("anonymousUser")) return 0;
        return cartItemRepository.findByUsername(username)
            .stream().mapToInt(CartItemEntity::getQuantity).sum();
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("pageTitle", "E-Commerce Store");
        model.addAttribute("brandName", "ShopEasy");
        model.addAttribute("heroTitle", "Welcome to Our Store");
        model.addAttribute("heroSubtitle", "Discover amazing products at unbeatable prices!");
        model.addAttribute("featuredTitle", "Featured Products");
        model.addAttribute("currentYear", Year.now().getValue());
        model.addAttribute("cartCount", getCartCount());

        var products = java.util.List.of(
            new Product("Wireless Headphones", "Premium sound quality with noise cancellation", 99.99, "/images/headphones.svg"),
            new Product("Smart Watch", "Track your health and stay connected", 149.99, "/images/watch.svg"),
            new Product("Bluetooth Speaker", "Portable speaker with rich bass", 59.99, "/images/speaker.svg"),
            new Product("Ultrabook Laptop", "Lightweight laptop for work and play", 899.99, "/images/laptop.svg"),
            new Product("Digital Camera", "Capture every moment in stunning detail", 449.99, "/images/camera.svg"),
            new Product("Android Tablet", "Perfect for entertainment and productivity", 329.99, "/images/tablet.svg")
        );
        model.addAttribute("products", products);

        var newArrivals = java.util.List.of(
            new Product("USB-C Cable", "Fast charging data cable", 14.99, "/images/headphones.svg"),
            new Product("Wireless Earbuds", "Compact true-wireless earbuds", 79.99, "/images/headphones.svg"),
            new Product("4K Webcam", "Crystal-clear video for streaming", 129.99, "/images/camera.svg")
        );
        model.addAttribute("newArrivals", newArrivals);
        return "index";
    }

    @GetMapping("/products")
    public String products(Model model) {
        model.addAttribute("pageTitle", "Products - ShopEasy");
        model.addAttribute("brandName", "ShopEasy");
        model.addAttribute("currentYear", Year.now().getValue());
        model.addAttribute("cartCount", getCartCount());

        var products = java.util.List.of(
            new Product("Wireless Headphones", "Premium sound quality with noise cancellation", 99.99, "/images/headphones.svg"),
            new Product("Smart Watch", "Track your health and stay connected", 149.99, "/images/watch.svg"),
            new Product("Bluetooth Speaker", "Portable speaker with rich bass", 59.99, "/images/speaker.svg"),
            new Product("USB-C Cable", "Fast charging data cable", 14.99, "/images/headphones.svg"),
            new Product("Ultrabook Laptop", "Lightweight laptop for work and play", 899.99, "/images/laptop.svg"),
            new Product("Digital Camera", "Capture every moment in stunning detail", 449.99, "/images/camera.svg"),
            new Product("Android Tablet", "Perfect for entertainment and productivity", 329.99, "/images/tablet.svg")
        );
        model.addAttribute("products", products);
        return "products";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("pageTitle", "Contact Us - ShopEasy");
        model.addAttribute("brandName", "ShopEasy");
        model.addAttribute("currentYear", Year.now().getValue());
        model.addAttribute("cartCount", getCartCount());
        return "contact";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("pageTitle", "About Us - ShopEasy");
        model.addAttribute("brandName", "ShopEasy");
        model.addAttribute("currentYear", Year.now().getValue());
        model.addAttribute("cartCount", getCartCount());
        return "about";
    }
}

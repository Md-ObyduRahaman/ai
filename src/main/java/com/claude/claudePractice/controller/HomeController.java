package com.claude.claudePractice.controller;

import com.claude.claudePractice.model.CartItemEntity;
import com.claude.claudePractice.model.Product;
import com.claude.claudePractice.repository.CartItemRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Year;
import java.util.List;

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
    public String home(@RequestParam(defaultValue = "1") int page, Model model) {
        // Show loading indicator for initial page load
        model.addAttribute("isLoading", true);

        // Meta attributes for template (preserved from original)
        model.addAttribute("pageTitle", "E-Commerce Store");
        model.addAttribute("brandName", "ShopEasy");
        model.addAttribute("heroTitle", "Welcome to Our Store");
        model.addAttribute("heroSubtitle", "Discover amazing products at unbeatable prices!");
        model.addAttribute("featuredTitle", "Featured Products");
        model.addAttribute("currentYear", Year.now().getValue());
        model.addAttribute("cartCount", getCartCount());

        // Featured products (preserved from original)
        var products = java.util.List.of(
            new Product("Wireless Headphones", "Premium sound quality with noise cancellation", 99.99, "/images/headphones.svg"),
            new Product("Smart Watch", "Track your health and stay connected", 149.99, "/images/watch.svg"),
            new Product("Bluetooth Speaker", "Portable speaker with rich bass", 59.99, "/images/speaker.svg"),
            new Product("Ultrabook Laptop", "Lightweight laptop for work and play", 899.99, "/images/laptop.svg"),
            new Product("Digital Camera", "Capture every moment in stunning detail", 449.99, "/images/camera.svg"),
            new Product("Android Tablet", "Perfect for entertainment and productivity", 329.99, "/images/tablet.svg")
        );
        model.addAttribute("products", products);

        // Expand new arrivals to 8 products
        var newArrivalsProducts = java.util.List.of(
            new Product("USB-C Cable", "Fast charging data cable", 14.99, "/images/headphones.svg"),
            new Product("Wireless Earbuds", "Compact true-wireless earbuds", 79.99, "/images/headphones.svg"),
            new Product("4K Webcam", "Crystal-clear video for streaming", 129.99, "/images/camera.svg"),
            new Product("Rechargeable Power Bank", "20000mAh portable charger", 39.99, "/images/powerbank.svg"),
            new Product("Smart Speaker", "Portable Bluetooth speaker with voice assistant", 124.99, "/images/speaker.svg"),
            new Product("Laptop Backpack", "Waterproof 15-inch laptop carrier", 49.99, "/images/backpack.svg"),
            new Product("Wireless Charger Pad", "Qi-compatible charging pad", 29.99, "/images/charger.svg"),
            new Product("Bluetooth Mouse", "Ergonomic wireless mouse", 39.95, "/images/mouse.svg")
        );

        // Pagination logic
        int itemsPerPage = 3;
        int totalPages = (int) Math.ceil(newArrivalsProducts.size() / (double) itemsPerPage);
        int start = (page - 1) * itemsPerPage;
        int end = Math.min(start + itemsPerPage, newArrivalsProducts.size());
        List<Product> currentPage = newArrivalsProducts.subList(start, end);
        int currentPageNum = page;

        // Add attributes for pagination controls
        model.addAttribute("newArrivals", currentPage);
        model.addAttribute("currentPage", currentPageNum);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("hasPrev", currentPageNum > 1);
        model.addAttribute("hasNext", currentPageNum < totalPages);

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

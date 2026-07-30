package com.claude.claudePractice.controller;

import com.claude.claudePractice.model.CartItemEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.claude.claudePractice.model.Product;
import com.claude.claudePractice.repository.CartItemRepository;
import com.claude.claudePractice.service.ProductService;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class HomeController {

    private final ProductService productService;
    @Autowired
    private final CartItemRepository cartItemRepository;

    @Autowired
    public HomeController(ProductService productService, CartItemRepository cartItemRepository) {
        this.productService = productService;
        this.cartItemRepository = cartItemRepository;
    }

    private int getCartCount() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        if (username.equals("anonymousUser")) return 0;
        return cartItemRepository.findByUsername(username).stream()
            .mapToInt(CartItemEntity::getQuantity)
            .sum();
    }

    @GetMapping("/")
    public String home(@RequestParam(defaultValue = "1") int page, Model model) {
        int itemsPerPage = 8;
        List<Product> newArrivalsProducts = productService.getProductsByPage(page - 1, itemsPerPage);
        model.addAttribute("newArrivals", newArrivalsProducts);

        int totalItems = productService.countAllProducts();
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("hasPrev", page > 1);
        model.addAttribute("hasNext", page < totalPages);
        model.addAttribute("cartCount", getCartCount());

        return "index";
    }

    @GetMapping("/products")
    public String products(Model model, @RequestParam(defaultValue = "1") int page) {
        int itemsPerPage = 6;
        List<Product> products = productService.getProductsByPage(page - 1, itemsPerPage);
        model.addAttribute("products", products);

        int totalItems = productService.countAllProducts();
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("hasPrev", page > 1);
        model.addAttribute("hasNext", page < totalPages);

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
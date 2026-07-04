package com.claude.claudePractice.controller;

import com.claude.claudePractice.model.CartItem;
import com.claude.claudePractice.model.CartItemEntity;
import com.claude.claudePractice.repository.CartItemRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class CartController {

    private final CartItemRepository cartItemRepository;

    public CartController(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    private String currentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private List<CartItem> getCartItems() {
        return cartItemRepository.findByUsername(currentUser())
            .stream().map(e -> new CartItem(e.getProductName(), e.getImageUrl(), e.getPrice(), e.getQuantity()))
            .collect(Collectors.toList());
    }

    private int getCartCount() {
        return cartItemRepository.findByUsername(currentUser())
            .stream().mapToInt(CartItemEntity::getQuantity).sum();
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam String name, @RequestParam String imageUrl,
                            @RequestParam double price) {
        String username = currentUser();
        var existing = cartItemRepository.findByUsernameAndProductName(username, name);
        if (existing.isPresent()) {
            CartItemEntity item = existing.get();
            item.setQuantity(item.getQuantity() + 1);
            cartItemRepository.save(item);
        } else {
            cartItemRepository.save(new CartItemEntity(username, name, imageUrl, price, 1));
        }
        return "redirect:/cart";
    }

    @GetMapping("/cart")
    public String viewCart(Model model) {
        model.addAttribute("pageTitle", "Cart - ShopEasy");
        model.addAttribute("brandName", "ShopEasy");
        model.addAttribute("currentYear", Year.now().getValue());
        model.addAttribute("cartCount", getCartCount());

        List<CartItem> cart = getCartItems();
        model.addAttribute("cartItems", cart);
        double total = cart.stream().mapToDouble(CartItem::getTotal).sum();
        model.addAttribute("cartTotal", total);
        return "cart";
    }

    @PostMapping("/cart/update")
    public String updateCart(@RequestParam String name, @RequestParam int quantity) {
        String username = currentUser();
        var existing = cartItemRepository.findByUsernameAndProductName(username, name);
        existing.ifPresent(item -> {
            if (quantity <= 0) cartItemRepository.delete(item);
            else { item.setQuantity(quantity); cartItemRepository.save(item); }
        });
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam String name) {
        String username = currentUser();
        cartItemRepository.findByUsernameAndProductName(username, name)
            .ifPresent(cartItemRepository::delete);
        return "redirect:/cart";
    }
}

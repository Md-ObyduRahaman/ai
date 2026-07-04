package com.claude.claudePractice.repository;

import com.claude.claudePractice.model.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {
    List<CartItemEntity> findByUsername(String username);
    Optional<CartItemEntity> findByUsernameAndProductName(String username, String productName);
    void deleteByUsername(String username);
}

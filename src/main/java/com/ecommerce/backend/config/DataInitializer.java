package com.ecommerce.backend.config;

import com.ecommerce.backend.model.Product;
import com.ecommerce.backend.model.User;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final double PRICE_INCREASE_MULTIPLIER = 1.15;

    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    private double increasedPrice(double price) {
        return Math.round(price * PRICE_INCREASE_MULTIPLIER * 100.0) / 100.0;
    }

    public DataInitializer(UserRepository userRepository, ProductRepository productRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        // Seed users if empty
        if (userRepository.count() == 0) {
            User admin = new User("Admin", "admin@shop.com", "admin123", "ADMIN");
            User user = new User("John Doe", "user@shop.com", "user123", "USER");
            userRepository.saveAll(List.of(admin, user));
            System.out.println(">>> Sample users seeded: admin@shop.com / admin123, user@shop.com / user123");
        }

        // Seed products if empty
        if (productRepository.count() == 0) {
            List<Product> products = Arrays.asList(
                new Product(
                    "Wireless Noise-Canceling Headphones",
                    "Premium over-ear headphones with active noise cancellation, 30-hour battery life, and crystal-clear sound.",
                    increasedPrice(149.99),
                    25,
                    "Electronics",
                    "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600"
                ),
                new Product(
                    "Smart Fitness Watch",
                    "Fitness tracker with heart rate monitor, GPS, sleep tracking, and water resistance up to 50 meters.",
                    increasedPrice(89.99),
                    40,
                    "Electronics",
                    "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600"
                ),
                new Product(
                    "Mechanical Gaming Keyboard",
                    "RGB backlit mechanical keyboard with tactile switches, anti-ghosting, and ergonomic wrist rest.",
                    increasedPrice(79.99),
                    15,
                    "Electronics",
                    "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600"
                ),
                new Product(
                    "Minimalist Leather Backpack",
                    "Durable water-resistant leather backpack with padded laptop sleeve for work and travel.",
                    increasedPrice(65.50),
                    30,
                    "Fashion",
                    "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=600"
                ),
                new Product(
                    "Classic Denim Jacket",
                    "Timeless style denim jacket crafted with high-quality organic cotton for all seasons.",
                    increasedPrice(54.00),
                    20,
                    "Fashion",
                    "https://images.unsplash.com/photo-1576995853123-5a10305d93c0?w=600"
                ),
                new Product(
                    "Ergonomic Office Chair",
                    "High-back mesh chair with lumbar support, adjustable headrest, and breathable material.",
                    increasedPrice(199.99),
                    12,
                    "Home & Office",
                    "https://images.unsplash.com/photo-1580481077111-e6e226e6443d?w=600"
                ),
                new Product(
                    "Stainless Steel Water Bottle",
                    "Insulated vacuum flask keeps beverages ice cold for 24 hours or piping hot for 12 hours.",
                    increasedPrice(24.99),
                    50,
                    "Home & Office",
                    "https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=600"
                ),
                new Product(
                    "Wireless Bluetooth Speaker",
                    "Compact portable speaker with rich bass, 360-degree surround sound, and IPX7 waterproofing.",
                    increasedPrice(45.00),
                    35,
                    "Electronics",
                    "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=600"
                )
            );
            productRepository.saveAll(products);
            System.out.println(">>> Sample products seeded successfully!");
        }
    }
}

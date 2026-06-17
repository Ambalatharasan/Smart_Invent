package com.stockwise.api.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.stockwise.api.entity.ActivityLog;
import com.stockwise.api.entity.ActivityType;
import com.stockwise.api.entity.InventoryItem;
import com.stockwise.api.entity.Role;
import com.stockwise.api.entity.StoreManager;
import com.stockwise.api.entity.UserAccount;
import com.stockwise.api.entity.UserProfileData;
import com.stockwise.api.repository.ActivityLogRepository;
import com.stockwise.api.repository.InventoryItemRepository;
import com.stockwise.api.repository.StoreManagerRepository;
import com.stockwise.api.repository.UserAccountRepository;
import com.stockwise.api.repository.UserProfileRepository;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(
            UserAccountRepository userAccountRepository,
            UserProfileRepository userProfileRepository,
            InventoryItemRepository inventoryItemRepository,
            StoreManagerRepository storeManagerRepository,
            ActivityLogRepository activityLogRepository,
            PasswordEncoder passwordEncoder,
            Environment environment,
            @Value("${stockwise.seed.users.enabled:false}") boolean seedUsersEnabled
    ) {
        return args -> {
            if (userAccountRepository.count() == 0 && seedUsersEnabled) {
                List<UserAccount> seedUsers = new ArrayList<>();
                addSeedUser(seedUsers, environment, passwordEncoder, "admin", "Smart Invent Admin", Role.ADMIN);
                addSeedUser(seedUsers, environment, passwordEncoder, "manager", "Operations Manager", Role.MANAGER);
                addSeedUser(seedUsers, environment, passwordEncoder, "viewer", "Inventory Viewer", Role.VIEWER);
                if (!seedUsers.isEmpty()) {
                    seedUsers.forEach(UserAccount::markEmailVerified);
                    userAccountRepository.saveAll(seedUsers);
                }
            }

            userAccountRepository.findAll().forEach(user -> {
                if (!userProfileRepository.existsByUserAccount(user)) {
                    userProfileRepository.save(new UserProfileData(user, user.getName(), null, user.getRole().name()));
                }
            });

            if (storeManagerRepository.count() == 0) {
                storeManagerRepository.saveAll(List.of(
                        new StoreManager("Elena Silva", "Operations Lead", "Operations", "elena@smartinvent.local", "555-0100", "Full day", "Available", "Exception review, supplier escalation, and daily operating rhythm"),
                        new StoreManager("Mira Patel", "Grocery Manager", "Grocery", "mira@smartinvent.local", "555-0101", "Morning", "Available", "Fresh grocery checks, aisle stock health, and replenishment approvals"),
                        new StoreManager("Noah Chen", "Home Goods Manager", "Home Goods", "noah@smartinvent.local", "555-0102", "Afternoon", "In review", "Home goods cycle counts, damaged stock review, and shelf capacity planning"),
                        new StoreManager("Tara Hughes", "Body Care Manager", "Body Care", "tara@smartinvent.local", "555-0103", "Morning", "Available", "Body care assortment, vendor follow-ups, and low-stock exceptions"),
                        new StoreManager("Owen Brooks", "Lifestyle Manager", "Lifestyle", "owen@smartinvent.local", "555-0104", "Afternoon", "Available", "Seasonal lifestyle inventory, display readiness, and stockroom rotation"),
                        new StoreManager("Priya Nair", "Stationery Manager", "Stationery", "priya@smartinvent.local", "555-0105", "Evening", "Available", "Stationery replenishment, back-to-school readiness, and shrinkage checks"),
                        new StoreManager("Marcus Reed", "Apparel Manager", "Apparel", "marcus@smartinvent.local", "555-0106", "Morning", "Follow-up", "Apparel rack coverage, sizing gaps, and returns monitoring")
                ));
            }

            if (inventoryItemRepository.count() == 0) {
                inventoryItemRepository.saveAll(List.of(
                        item("ITEM-0001", "Colombian Whole Bean Coffee", "Grocery", "Andes Roasters", 18, 24, "6.90", "12.99", 5, "Aisle 1", "Best seller, monitor weekend demand", "2026-05-19", 36),
                        item("ITEM-0002", "Organic Lavender Soap", "Body Care", "Botanic Works", 0, 18, "2.10", "5.49", 7, "Shelf C2", "Out of stock until next order", "2026-05-10", 24),
                        item("ITEM-0003", "Reusable Produce Bags", "Home Goods", "EcoSupply Co", 42, 20, "3.45", "8.99", 6, "Aisle 4", "High margin add-on item", "2026-05-23", 40),
                        item("ITEM-0004", "Local Honey Jar", "Grocery", "Meadow Apiary", 11, 16, "4.20", "9.99", 3, "Aisle 2", "Fast moving local product", "2026-05-18", 20),
                        item("ITEM-0005", "Premium Gel Pen Pack", "Stationery", "Northline Paper", 64, 20, "1.80", "4.99", 4, "Shelf S1", "Keep near checkout", "2026-05-24", 60),
                        item("ITEM-0006", "Kids Rain Poncho", "Apparel", "Trailwear Goods", 7, 14, "5.95", "14.99", 12, "Rack 4", "Seasonal weather spike risk", "2026-05-12", 18),
                        item("ITEM-0007", "Soy Candle Citrus", "Lifestyle", "Glow Studio", 38, 12, "4.75", "11.99", 8, "Display L3", "Strong gifting item", "2026-05-22", 24),
                        item("ITEM-0008", "Stoneware Mug Set", "Home Goods", "Clay & Co", 23, 10, "9.50", "24.99", 9, "Aisle 5", "Fragile stock, inspect cartons", "2026-05-20", 16)
                ));
            }

            if (activityLogRepository.count() == 0) {
                activityLogRepository.saveAll(List.of(
                        new ActivityLog(ActivityType.NOTE, "Backend initialized with seed operating data", null, null, null, "system"),
                        new ActivityLog(ActivityType.RESTOCK_ORDERED, "Review required for Organic Lavender Soap", "ITEM-0002", 24, LocalDate.parse("2026-05-10"), "system"),
                        new ActivityLog(ActivityType.STOCK_ADJUSTED, "Low stock watch opened for Kids Rain Poncho", "ITEM-0006", 7, LocalDate.parse("2026-05-12"), "system")
                ));
            }
        };
    }

    private void addSeedUser(
            List<UserAccount> seedUsers,
            Environment environment,
            PasswordEncoder passwordEncoder,
            String key,
            String defaultName,
            Role role
    ) {
        String email = environment.getProperty("stockwise.seed." + key + ".email", "").trim().toLowerCase(Locale.ROOT);
        String password = environment.getProperty("stockwise.seed." + key + ".password", "");
        if (email.isBlank() || password.isBlank()) {
            return;
        }

        String name = environment.getProperty("stockwise.seed." + key + ".name", defaultName).trim();
        seedUsers.add(new UserAccount(name, email, passwordEncoder.encode(password), role));
    }

    private InventoryItem item(
            String sku,
            String name,
            String category,
            String supplier,
            int quantity,
            int reorderPoint,
            String unitCost,
            String retailPrice,
            int leadTimeDays,
            String location,
            String notes,
            String lastRestockDate,
            int lastRestockQuantity
    ) {
        return new InventoryItem(
                sku,
                name,
                category,
                supplier,
                quantity,
                reorderPoint,
                new BigDecimal(unitCost),
                new BigDecimal(retailPrice),
                leadTimeDays,
                location,
                notes,
                LocalDate.parse(lastRestockDate),
                lastRestockQuantity
        );
    }
}

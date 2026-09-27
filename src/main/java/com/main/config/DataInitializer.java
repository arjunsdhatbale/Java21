package com.main.config;

import com.main.model.entity.Order;
import com.main.model.entity.OrderItem;
import com.main.model.entity.OrderStatus;
import com.main.model.entity.Payment;
import com.main.model.entity.PaymentMethod;
import com.main.model.entity.PaymentStatus;
import com.main.model.entity.Product;
import com.main.model.entity.User;
import com.main.repo.OrderRepository;
import com.main.repo.PaymentRepository;
import com.main.repo.ProductRepository;
import com.main.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public void run(String... args) {
        seedUsers();
        seedProducts();
        seedOrders();
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            log.info("Seeding initial users into database...");
            List<User> initialUsers = List.of(
                    User.builder()
                            .firstName("Arjun")
                            .lastName("Dhatbale")
                            .email("arjun@example.com")
                            .password("Password123")
                            .phone("9876543210")
                            .role(User.UserRole.ADMIN)
                            .status(User.UserStatus.ACTIVE)
                            .build(),
                    User.builder()
                            .firstName("Sarah")
                            .lastName("Connor")
                            .email("sarah.connor@example.com")
                            .password("Password123")
                            .phone("9141555267")
                            .role(User.UserRole.MANAGER)
                            .status(User.UserStatus.ACTIVE)
                            .build(),
                    User.builder()
                            .firstName("John")
                            .lastName("Doe")
                            .email("john.doe@example.com")
                            .password("Password123")
                            .phone("9120255501")
                            .role(User.UserRole.USER)
                            .status(User.UserStatus.INACTIVE)
                            .build(),
                    User.builder()
                            .firstName("Priya")
                            .lastName("Sharma")
                            .email("priya.sharma@example.com")
                            .password("Password123")
                            .phone("9123456780")
                            .role(User.UserRole.MANAGER)
                            .status(User.UserStatus.ACTIVE)
                            .build(),
                    User.builder()
                            .firstName("Michael")
                            .lastName("Scott")
                            .email("michael.scott@example.com")
                            .password("Password123")
                            .phone("9157055501")
                            .role(User.UserRole.USER)
                            .status(User.UserStatus.BLOCKED)
                            .build()
            );
            userRepository.saveAll(initialUsers);
            log.info("Seeded {} users successfully.", initialUsers.size());
        }
    }

    private void seedProducts() {
        if (productRepository.count() == 0) {
            log.info("Seeding initial products into database...");
            List<Product> initialProducts = List.of(
                    Product.builder()
                            .name("Logitech MX Master 3S")
                            .description("Advanced Wireless Mouse with quiet clicks and 8K DPI sensor")
                            .price(new BigDecimal("8999.00"))
                            .stock(24)
                            .category("Electronics")
                            .imageUrl("https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=100&auto=format&fit=crop&q=60")
                            .status(Product.ProductStatus.ACTIVE)
                            .build(),
                    Product.builder()
                            .name("Mechanical Gaming Keyboard")
                            .description("RGB Backlit Hot-swappable mechanical keyboard")
                            .price(new BigDecimal("5499.00"))
                            .stock(12)
                            .category("Electronics")
                            .imageUrl("https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=100&auto=format&fit=crop&q=60")
                            .status(Product.ProductStatus.ACTIVE)
                            .build(),
                    Product.builder()
                            .name("Ergonomic Office Chair")
                            .description("High-back mesh chair with lumbar support")
                            .price(new BigDecimal("14999.00"))
                            .stock(5)
                            .category("Furniture")
                            .imageUrl("https://images.unsplash.com/photo-1580481077195-c9f1388efd38?w=100&auto=format&fit=crop&q=60")
                            .status(Product.ProductStatus.ACTIVE)
                            .build(),
                    Product.builder()
                            .name("Sony WH-1000XM5 Headphones")
                            .description("Industry-leading noise cancelling wireless headphones")
                            .price(new BigDecimal("26990.00"))
                            .stock(0)
                            .category("Electronics")
                            .imageUrl("https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=100&auto=format&fit=crop&q=60")
                            .status(Product.ProductStatus.OUT_OF_STOCK)
                            .build(),
                    Product.builder()
                            .name("Standing Desk Converter")
                            .description("Adjustable height sit-to-stand dual monitor riser")
                            .price(new BigDecimal("11499.00"))
                            .stock(8)
                            .category("Furniture")
                            .imageUrl("https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?w=100&auto=format&fit=crop&q=60")
                            .status(Product.ProductStatus.ACTIVE)
                            .build(),
                    Product.builder()
                            .name("USB-C Multiport Adapter")
                            .description("7-in-1 Hub with 4K HDMI, 100W PD, and SD Card Reader")
                            .price(new BigDecimal("2499.00"))
                            .stock(3)
                            .category("Accessories")
                            .imageUrl("")
                            .status(Product.ProductStatus.INACTIVE)
                            .build()
            );
            productRepository.saveAll(initialProducts);
            log.info("Seeded {} products successfully.", initialProducts.size());
        }
    }

    private void seedOrders() {
        if (orderRepository.count() == 0) {
            log.info("Seeding initial orders into database...");
            List<User> users = userRepository.findAll();
            List<Product> products = productRepository.findAll();

            if (!users.isEmpty() && products.size() >= 2) {
                User user = users.get(0);
                Product p1 = products.get(0);
                Product p2 = products.get(1);

                BigDecimal amount1 = p1.getPrice().multiply(BigDecimal.valueOf(1));
                BigDecimal amount2 = p2.getPrice().multiply(BigDecimal.valueOf(2));
                BigDecimal total = amount1.add(amount2);

                Order order1 = Order.builder()
                        .orderNumber("ORD-20260928-1001")
                        .user(user)
                        .status(OrderStatus.CONFIRMED)
                        .totalAmount(total)
                        .shippingAddress("123 Tech Park, Phase 1, Bangalore, Karnataka - 560100")
                        .contactPhone("9876543210")
                        .paymentMethod("UPI")
                        .paymentStatus(PaymentStatus.PAID)
                        .notes("Please call before delivery")
                        .items(new ArrayList<>())
                        .build();

                OrderItem item1 = OrderItem.builder()
                        .order(order1)
                        .product(p1)
                        .productName(p1.getName())
                        .productPrice(p1.getPrice())
                        .quantity(1)
                        .subtotal(amount1)
                        .build();

                OrderItem item2 = OrderItem.builder()
                        .order(order1)
                        .product(p2)
                        .productName(p2.getName())
                        .productPrice(p2.getPrice())
                        .quantity(2)
                        .subtotal(amount2)
                        .build();

                order1.addItem(item1);
                order1.addItem(item2);
                Order savedOrder = orderRepository.save(order1);
                log.info("Seeded initial order successfully: {}", savedOrder.getOrderNumber());

                if (paymentRepository.count() == 0) {
                    Payment payment1 = Payment.builder()
                            .transactionId("TXN-20260928-1001")
                            .order(savedOrder)
                            .user(user)
                            .amount(total)
                            .currency("INR")
                            .paymentMethod(PaymentMethod.UPI)
                            .paymentStatus(PaymentStatus.PAID)
                            .gatewayReference("GW-UPI-SUCCESS-9912")
                            .paymentDate(LocalDateTime.now())
                            .notes("Initial seeded payment via UPI")
                            .build();

                    paymentRepository.save(payment1);
                    log.info("Seeded initial payment successfully: {}", payment1.getTransactionId());
                }
            }
        }
    }
}

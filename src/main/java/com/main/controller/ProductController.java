package com.main.controller;

import com.main.model.dto.ProductRequestDto;
import com.main.model.dto.ProductResponseDto;
import com.main.service.ProductService;
import com.main.service.ProductServiceImpl;
import com.main.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    Logger logger = LoggerFactory.getLogger(ProductController.class);
    private final ProductService productService;
    private final ProductServiceImpl productServiceImpl;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponseDto>> createProduct(
            @Valid @RequestBody ProductRequestDto dto) {
        logger.info("Request received to Create product.");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully",
                        productService.createProduct(dto)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductById(
            @PathVariable Long id) {
        logger.info("Request received to get project by id : {}.", id);
        return ResponseEntity.ok(ApiResponse.success("Product fetched successfully",
                productService.getProductById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getAllProducts() {
        logger.info("Request received to get all products.");
        return ResponseEntity.ok(ApiResponse.success("Products fetched successfully",
                productService.getAllProducts()));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getProductsByCategory(
            @PathVariable String category) {
        logger.info("Request received to get product by category : {}.", category);
        return ResponseEntity.ok(ApiResponse.success("Products fetched by category",
                productService.getProductsByCategory(category)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getAllCategories() {
        logger.info("Request received to get all categories.");
        return ResponseEntity.ok(ApiResponse.success("Categories fetched successfully",
                productService.getAllCategories()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDto dto) {
        logger.info("Request received to  update product by id : {}.", id);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully",
                productService.updateProduct(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        logger.info("Request received to delete product by id : {}.", id);
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> searchProducts(
            @RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success("Search results",
                productServiceImpl.search(keyword)));
    }
}

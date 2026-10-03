package com.main.product;

import java.util.List;

public interface ProductService {
    ProductResponseDto createProduct(ProductRequestDto dto);
    ProductResponseDto getProductById(Long id);
    List<ProductResponseDto> getAllProducts();
    List<ProductResponseDto> getProductsByCategory(String category);
    ProductResponseDto updateProduct(Long id, ProductRequestDto dto);
    void deleteProduct(Long id);
    List<String> getAllCategories();
    List<ProductResponseDto> search(String keyword);
}

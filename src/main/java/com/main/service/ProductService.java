package com.main.service;


import com.main.model.dto.ProductRequestDto;
import com.main.model.dto.ProductResponseDto;

import java.util.List;

public interface ProductService {
    ProductResponseDto createProduct(ProductRequestDto dto);
    ProductResponseDto getProductById(Long id);
    List<ProductResponseDto> getAllProducts();
    List<ProductResponseDto> getProductsByCategory(String category);
    ProductResponseDto updateProduct(Long id, ProductRequestDto dto);
    void deleteProduct(Long id);

    List<String> getAllCategories();
}
package com.main.mapper;


 import com.main.model.dto.ProductRequestDto;
 import com.main.model.dto.ProductResponseDto;
 import com.main.model.entity.Product;
 import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequestDto dto) {
        return Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .category(dto.getCategory())
                .imageUrl(dto.getImageUrl())
                .status(dto.getStatus() != null
                        ? Product.ProductStatus.valueOf(dto.getStatus())
                        : Product.ProductStatus.ACTIVE)
                .build();
    }

    public ProductResponseDto toDto(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .status(product.getStatus().name())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
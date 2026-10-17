package com.main.product;

import java.util.List;
import java.util.Set;

public interface ProductBulkRepository {
    Set<String> findExistingProductNames(Set<String> candidateNames);
    void batchInsert(List<Product> entities);
}

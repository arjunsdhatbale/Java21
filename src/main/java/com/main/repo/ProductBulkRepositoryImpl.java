package com.main.repo;

import com.main.model.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductBulkRepositoryImpl implements ProductBulkRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public Set<String> findExistingProductNames(Set<String> candidateNames) {
        if (candidateNames == null || candidateNames.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> lowerNames = candidateNames.stream()
                .filter(n -> n != null && !n.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        if (lowerNames.isEmpty()) {
            return Collections.emptySet();
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("names", lowerNames);

        String sql = "SELECT LOWER(name) FROM products WHERE LOWER(name) IN (:names)";
        List<String> existing = namedParameterJdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> rs.getString(1).toLowerCase()
        );

        return new HashSet<>(existing);
    }

    @Override
    public void batchInsert(List<Product> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO products (name, description, price, stock, category, image_url, status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Product product = entities.get(i);
                ps.setString(1, product.getName());
                ps.setString(2, product.getDescription());
                ps.setBigDecimal(3, product.getPrice());
                ps.setInt(4, product.getStock() != null ? product.getStock() : 0);
                ps.setString(5, product.getCategory());
                ps.setString(6, product.getImageUrl());
                ps.setString(7, product.getStatus() != null ? product.getStatus().name() : Product.ProductStatus.ACTIVE.name());
                Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                ps.setTimestamp(8, now);
                ps.setTimestamp(9, now);
            }

            @Override
            public int getBatchSize() {
                return entities.size();
            }
        });
    }
}

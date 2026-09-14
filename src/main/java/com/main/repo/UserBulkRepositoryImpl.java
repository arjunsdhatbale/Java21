package com.main.repo;

import com.main.model.entity.User;
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
public class UserBulkRepositoryImpl implements UserBulkRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public Set<String> findExistingEmails(Set<String> candidateEmails) {
        if (candidateEmails == null || candidateEmails.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> lowerEmails = candidateEmails.stream()
                .filter(e -> e != null && !e.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        if (lowerEmails.isEmpty()) {
            return Collections.emptySet();
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("emails", lowerEmails);

        String sql = "SELECT LOWER(email) FROM users WHERE LOWER(email) IN (:emails)";
        List<String> existing = namedParameterJdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> rs.getString(1).toLowerCase()
        );

        return new HashSet<>(existing);
    }

    @Override
    public void batchInsert(List<User> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO users (first_name, last_name, email, password, phone, role, status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                User user = entities.get(i);
                ps.setString(1, user.getFirstName());
                ps.setString(2, user.getLastName());
                ps.setString(3, user.getEmail());
                ps.setString(4, user.getPassword());
                ps.setString(5, user.getPhone());
                ps.setString(6, user.getRole() != null ? user.getRole().name() : User.UserRole.USER.name());
                ps.setString(7, user.getStatus() != null ? user.getStatus().name() : User.UserStatus.ACTIVE.name());
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

package com.main.repo;

import com.main.model.entity.User;

import java.util.List;
import java.util.Set;

public interface UserBulkRepository {
    Set<String> findExistingEmails(Set<String> candidateEmails);
    void batchInsert(List<User> entities);
}

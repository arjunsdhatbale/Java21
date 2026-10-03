package com.main.shared;

import java.util.List;

public interface SearchService<T> {
    List<T> search(String keyword);
}

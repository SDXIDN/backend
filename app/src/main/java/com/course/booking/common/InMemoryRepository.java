package com.course.booking.common;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public abstract class InMemoryRepository<T extends BaseEntity> {
    private final Map<Long, T> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public List<T> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(BaseEntity::getId))
                .toList();
    }

    public Optional<T> findById(Long id) {
        return id == null ? Optional.empty() : Optional.ofNullable(store.get(id));
    }

    public boolean existsById(Long id) {
        return id != null && store.containsKey(id);
    }

    public T save(T entity) {
        if (entity.getId() == null) {
            entity.setId(sequence.incrementAndGet());
        }
        store.put(entity.getId(), entity);
        return entity;
    }

    public void deleteById(Long id) {
        store.remove(id);
    }
}

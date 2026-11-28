package com.course.booking.common;

import java.util.List;

public abstract class CrudService<T extends BaseEntity> {
    private final InMemoryRepository<T> repository;
    private final String entityName;

    protected CrudService(InMemoryRepository<T> repository, String entityName) {
        this.repository = repository;
        this.entityName = entityName;
    }

    public List<T> findAll() {
        return repository.findAll();
    }

    public T findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException(entityName + " not found with id: " + id));
    }

    public T create(T entity) {
        entity.setId(null);
        applyDefaults(entity);
        validate(entity);
        checkConstraints(entity);
        return repository.save(entity);
    }

    public T replace(Long id, T entity) {
        findById(id);
        entity.setId(id);
        applyDefaults(entity);
        validate(entity);
        checkConstraints(entity);
        return repository.save(entity);
    }

    public T patch(Long id, T patch) {
        T merged = merge(findById(id), patch);
        merged.setId(id);
        validate(merged);
        checkConstraints(merged);
        return repository.save(merged);
    }

    public void delete(Long id) {
        findById(id);
        checkCanDelete(id);
        repository.deleteById(id);
    }

    protected void applyDefaults(T entity) {
    }

    protected abstract void validate(T entity);

    protected void checkConstraints(T entity) {
    }

    protected abstract T merge(T existing, T patch);

    protected void checkCanDelete(Long id) {
    }

    protected static <V> V orElse(V patchValue, V existingValue) {
        return patchValue != null ? patchValue : existingValue;
    }

    protected static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    protected static void requireNotNull(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
    }
}

package com.hsj.service;

import com.hsj.dto.category.CategoryCreateRequest;
import com.hsj.dto.category.CategoryResponse;
import com.hsj.dto.category.CategoryUpdateRequest;
import com.hsj.entity.Category;
import com.hsj.exception.DuplicateException;
import com.hsj.exception.ErrorCode;
import com.hsj.exception.NotFoundException;
import com.hsj.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        if (categoryRepository.existsByNameAndDeletedFalse(request.getName())) {
            throw new DuplicateException(ErrorCode.DUPLICATE_CATEGORY);
        }

        Category parent = null;
        if (request.getParentId() != null) {
            parent = findCategoryOrThrow(request.getParentId());
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .sortOrder(request.getSortOrder())
                .parent(parent)
                .build();

        return CategoryResponse.from(categoryRepository.save(category));
    }

    public List<CategoryResponse> findAllTree() {
        List<Category> roots = categoryRepository.findByParentIsNullAndDeletedFalseOrderBySortOrder();
        return roots.stream()
                .map(CategoryResponse::withChildren)
                .toList();
    }

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllByDeletedFalseOrderBySortOrder().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse findById(Long id) {
        Category category = findCategoryOrThrow(id);
        return CategoryResponse.withChildren(category);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryUpdateRequest request) {
        Category category = findCategoryOrThrow(id);

        if (request.getParentId() != null) {
            Category parent = findCategoryOrThrow(request.getParentId());
            category.changeParent(parent);
        }

        category.updateInfo(request.getName(), request.getDescription(), request.getSortOrder());
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = findCategoryOrThrow(id);
        category.softDelete();
    }

    private Category findCategoryOrThrow(Long id) {
        return categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}

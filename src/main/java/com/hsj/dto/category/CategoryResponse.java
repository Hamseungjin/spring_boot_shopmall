package com.hsj.dto.category;

import com.hsj.entity.Category;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CategoryResponse {

    private Long id;
    private String name;
    private String description;
    private Integer sortOrder;
    private Long parentId;
    private String parentName;
    private List<CategoryResponse> children;

    public static CategoryResponse from(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .sortOrder(category.getSortOrder())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .build();
    }

    public static CategoryResponse withChildren(Category category) {
        List<CategoryResponse> childResponses = category.getChildren().stream()
                .filter(c -> !c.isDeleted())
                .map(CategoryResponse::withChildren)
                .toList();

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .sortOrder(category.getSortOrder())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .children(childResponses)
                .build();
    }
}

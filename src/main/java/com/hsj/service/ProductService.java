package com.hsj.service;

import com.hsj.dto.common.PageResponse;
import com.hsj.dto.product.*;
import com.hsj.entity.Category;
import com.hsj.entity.Product;
import com.hsj.exception.ErrorCode;
import com.hsj.exception.NotFoundException;
import com.hsj.repository.CategoryRepository;
import com.hsj.repository.ProductRepository;
import com.hsj.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StorageService storageService;

    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findByIdAndDeletedFalse(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .imageUrl(request.getImageUrl())
                .category(category)
                .build();

        Product saved = productRepository.save(product);
        log.info("상품 등록: id={}, name={}", saved.getId(), saved.getName());
        return ProductResponse.from(saved);
    }

    public ProductResponse findById(Long id) {
        Product product = findProductOrThrow(id);
        return ProductResponse.from(product);
    }

    public PageResponse<ProductResponse> findAll(Pageable pageable) {
        Page<ProductResponse> page = productRepository.findAllByDeletedFalse(pageable)
                .map(ProductResponse::from);
        return PageResponse.from(page);
    }

    public PageResponse<ProductResponse> search(ProductSearchCondition condition, Pageable pageable) {
        Page<ProductResponse> page = productRepository.searchProducts(condition, pageable)
                .map(ProductResponse::from);
        return PageResponse.from(page);
    }

    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request) {
        Product product = findProductOrThrow(id);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findByIdAndDeletedFalse(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND));
            product.changeCategory(category);
        }

        product.updateInfo(request.getName(), request.getDescription(),
                request.getPrice(), request.getImageUrl());

        log.info("상품 수정: id={}", id);
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findProductOrThrow(id);
        product.softDelete();
        log.info("상품 삭제(soft): id={}", id);
    }

    @Transactional
    public ProductResponse uploadImage(Long id, MultipartFile file) {
        Product product = findProductOrThrow(id);

        if (product.getImageUrl() != null) {
            storageService.delete(product.getImageUrl());
        }

        String storedPath = storageService.store(file, "products");
        String imageUrl = storageService.getFileUrl(storedPath);
        product.updateInfo(null, null, null, imageUrl);

        log.info("상품 이미지 업로드: productId={}, url={}", id, imageUrl);
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse addStock(Long id, int quantity) {
        Product product = findProductOrThrow(id);
        product.addStock(quantity);
        log.info("재고 추가: productId={}, +{}, total={}", id, quantity, product.getStockQuantity());
        return ProductResponse.from(product);
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}

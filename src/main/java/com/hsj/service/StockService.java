package com.hsj.service;

import com.hsj.entity.Product;
import com.hsj.exception.ErrorCode;
import com.hsj.exception.NotFoundException;
import com.hsj.exception.OutOfStockException;
import com.hsj.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private static final String STOCK_LOCK_PREFIX = "LOCK:STOCK:";
    private static final long WAIT_TIME = 5L;
    private static final long LEASE_TIME = 3L;

    private final ProductRepository productRepository;
    private final RedissonClient redissonClient;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deductStock(Long productId, int quantity) {
        RLock lock = redissonClient.getLock(STOCK_LOCK_PREFIX + productId);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS);
            if (!acquired) {
                throw new IllegalStateException(
                        "재고 차감 락 획득 실패: productId=" + productId);
            }

            Product product = productRepository.findByIdAndDeletedFalse(productId)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

            if (product.getStockQuantity() < quantity) {
                throw new OutOfStockException(
                        String.format("재고 부족: 상품=%s, 현재=%d, 요청=%d",
                                product.getName(), product.getStockQuantity(), quantity));
            }

            product.removeStock(quantity);
            productRepository.saveAndFlush(product);

            log.info("재고 차감 완료: productId={}, -{}, 잔여={}",
                    productId, quantity, product.getStockQuantity());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("재고 차감 중 인터럽트 발생", e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreStock(Long productId, int quantity) {
        RLock lock = redissonClient.getLock(STOCK_LOCK_PREFIX + productId);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS);
            if (!acquired) {
                throw new IllegalStateException(
                        "재고 복원 락 획득 실패: productId=" + productId);
            }

            Product product = productRepository.findByIdAndDeletedFalse(productId)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

            product.addStock(quantity);
            productRepository.saveAndFlush(product);

            log.info("재고 복원 완료: productId={}, +{}, 잔여={}",
                    productId, quantity, product.getStockQuantity());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("재고 복원 중 인터럽트 발생", e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}

package com.hsj.service;

import com.hsj.entity.Product;
import com.hsj.exception.NotFoundException;
import com.hsj.exception.OutOfStockException;
import com.hsj.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockService 단위 테스트 (분산 락)")
class StockServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @InjectMocks
    private StockService stockService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .name("테스트 상품")
                .price(BigDecimal.valueOf(10_000))
                .stockQuantity(100)
                .build();
    }

    // ═══════════════════════ deductStock ═══════════════════════

    @Test
    @DisplayName("deductStock: 정상 수량 차감 - 재고가 줄어들고 락이 해제된다")
    void deductStock_정상차감() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(productRepository.saveAndFlush(product)).thenReturn(product);

        stockService.deductStock(1L, 10);

        assertThat(product.getStockQuantity()).isEqualTo(90);
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("deductStock: 분산 락 획득 실패 시 IllegalStateException 발생")
    void deductStock_락획득실패_예외() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        assertThatThrownBy(() -> stockService.deductStock(1L, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("락 획득 실패");

        verify(rLock, never()).unlock();
    }

    @Test
    @DisplayName("deductStock: 상품을 찾을 수 없으면 NotFoundException 발생 + 락 해제")
    void deductStock_상품없음_NotFoundException() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(productRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.deductStock(99L, 5))
                .isInstanceOf(NotFoundException.class);

        verify(rLock).unlock();
    }

    @Test
    @DisplayName("deductStock: 재고 부족 시 OutOfStockException 발생 + 락 해제")
    void deductStock_재고부족_OutOfStockException() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));

        // 현재 재고 100, 200개 요청 → 부족
        assertThatThrownBy(() -> stockService.deductStock(1L, 200))
                .isInstanceOf(OutOfStockException.class);

        // 재고는 차감되지 않아야 한다
        assertThat(product.getStockQuantity()).isEqualTo(100);
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("deductStock: 인터럽트 발생 시 InterruptedException을 감싸서 IllegalStateException 발생")
    void deductStock_인터럽트_IllegalStateException() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class)))
                .thenThrow(new InterruptedException("테스트 인터럽트"));

        assertThatThrownBy(() -> stockService.deductStock(1L, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("인터럽트");
    }

    @Test
    @DisplayName("deductStock: 락 키 형식이 'LOCK:STOCK:{productId}'여야 한다")
    void deductStock_락키형식_검증() throws InterruptedException {
        when(redissonClient.getLock("LOCK:STOCK:7")).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(productRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(product));
        when(productRepository.saveAndFlush(product)).thenReturn(product);

        stockService.deductStock(7L, 1);

        verify(redissonClient).getLock("LOCK:STOCK:7");
    }

    // ═══════════════════════ restoreStock ═══════════════════════

    @Test
    @DisplayName("restoreStock: 정상 재고 복원 - 재고가 증가하고 락이 해제된다")
    void restoreStock_정상복원() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(productRepository.saveAndFlush(product)).thenReturn(product);

        stockService.restoreStock(1L, 10);

        assertThat(product.getStockQuantity()).isEqualTo(110);
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("restoreStock: 분산 락 획득 실패 시 IllegalStateException 발생")
    void restoreStock_락획득실패_예외() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        assertThatThrownBy(() -> stockService.restoreStock(1L, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("락 획득 실패");

        verify(rLock, never()).unlock();
    }

    @Test
    @DisplayName("restoreStock: 상품을 찾을 수 없으면 NotFoundException 발생 + 락 해제")
    void restoreStock_상품없음_NotFoundException() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(productRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.restoreStock(99L, 5))
                .isInstanceOf(NotFoundException.class);

        verify(rLock).unlock();
    }
}

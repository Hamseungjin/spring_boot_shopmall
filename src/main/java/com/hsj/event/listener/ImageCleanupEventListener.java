package com.hsj.event.listener;

import com.hsj.event.OldImageCleanupEvent;
import com.hsj.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 이전 상품 이미지 비동기 삭제 리스너.
 *
 * <pre>
 * - AFTER_COMMIT: 새 이미지 URL이 DB에 커밋된 후에만 삭제 → 업로드 실패 시 이전 이미지 보존
 * - 삭제 실패 시 경고 로그만 출력 (단순 고아 파일 → 영향도 낮음)
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageCleanupEventListener {

    private final StorageService storageService;

    @Async("eventLogExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OldImageCleanupEvent event) {
        try {
            storageService.delete(event.getImageUrl());
            log.debug("이전 이미지 삭제 완료: url={}", event.getImageUrl());
        } catch (Exception e) {
            log.warn("이전 이미지 삭제 실패 (고아 파일 잔존, 무시 가능): url={} - {}",
                    event.getImageUrl(), e.getMessage());
        }
    }
}

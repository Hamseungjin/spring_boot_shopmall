package com.hsj.event;

import lombok.Getter;

/**
 * 상품 이미지 교체 시 이전 이미지 파일을 비동기로 삭제하기 위한 이벤트.
 * 트랜잭션 커밋 이후에만 삭제가 실행되므로 새 이미지 저장 실패 시 이전 이미지는 보존된다.
 */
@Getter
public class OldImageCleanupEvent {

    private final String imageUrl;

    public OldImageCleanupEvent(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}

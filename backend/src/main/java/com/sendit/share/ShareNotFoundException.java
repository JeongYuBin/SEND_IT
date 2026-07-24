package com.sendit.share;

public class ShareNotFoundException extends RuntimeException {

    public ShareNotFoundException(Long id) {
        super("공유 콘텐츠를 찾을 수 없습니다: " + id);
    }
}


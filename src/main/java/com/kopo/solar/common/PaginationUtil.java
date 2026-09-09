package com.kopo.solar.common;

import org.springframework.data.domain.Page;

public class PaginationUtil {

    private static final int BLOCK_SIZE = 10;

    private PaginationUtil() {
    }

    // 현재 페이지가 속한 페이지 블록(1~10, 11~20 ...) 범위와 이전/다음 블록 존재 여부 계산
    public static PageBlock of(Page<?> page) {
        int totalPages = Math.max(page.getTotalPages(), 1);
        int startPage = (page.getNumber() / BLOCK_SIZE) * BLOCK_SIZE;
        int endPage = Math.min(startPage + BLOCK_SIZE - 1, totalPages - 1);
        boolean hasPrevBlock = startPage > 0;
        boolean hasNextBlock = endPage < totalPages - 1;
        return new PageBlock(startPage, endPage, hasPrevBlock, hasNextBlock, startPage - 1, endPage + 1);
    }

    public record PageBlock(int startPage, int endPage, boolean hasPrevBlock, boolean hasNextBlock,
                             int prevBlockPage, int nextBlockPage) {
    }
}

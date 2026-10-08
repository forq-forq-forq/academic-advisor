package kz.edu.sdu.advisor.model.dto;

import java.util.List;

public record CartDto(
        List<CartItemDto> items,
        int totalCredits,
        int maxCredits,
        String workloadState,
        String workloadColor,
        boolean empty
) {}


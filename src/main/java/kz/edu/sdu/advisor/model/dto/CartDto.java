package kz.edu.sdu.advisor.model.dto;

import java.util.List;
import java.util.stream.Collectors;

public record CartDto(
        List<CartItemDto> items,
        int totalCredits,
        int maxCredits,
        String workloadState,
        String workloadColor,
        boolean empty,
        String formattedCodes
) {
    public CartDto(
            List<CartItemDto> items,
            int totalCredits,
            int maxCredits,
            String workloadState,
            String workloadColor,
            boolean empty
    ) {
        this(
                items,
                totalCredits,
                maxCredits,
                workloadState,
                workloadColor,
                empty,
                items == null || items.isEmpty()
                        ? ""
                        : items.stream().map(CartItemDto::code).collect(Collectors.joining(", "))
        );
    }
}


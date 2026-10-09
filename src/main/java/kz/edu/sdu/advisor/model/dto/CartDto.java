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
        String formattedCodes,
        boolean exceedsLimit,
        int excessCredits,
        String limitWarning
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

    public CartDto(
            List<CartItemDto> items,
            int totalCredits,
            int maxCredits,
            String workloadState,
            String workloadColor,
            boolean empty,
            String formattedCodes
    ) {
        this(
                items,
                totalCredits,
                maxCredits,
                workloadState,
                workloadColor,
                empty,
                formattedCodes,
                totalCredits > maxCredits,
                Math.max(0, totalCredits - maxCredits),
                totalCredits > maxCredits
                        ? String.format("Credit limit exceeded: You have selected %d ECTS, which exceeds the maximum limit of %d ECTS by %d ECTS.",
                                totalCredits, maxCredits, totalCredits - maxCredits)
                        : null
        );
    }
}


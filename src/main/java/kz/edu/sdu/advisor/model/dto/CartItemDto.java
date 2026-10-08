package kz.edu.sdu.advisor.model.dto;

public record CartItemDto(
        Long id,
        Long courseId,
        String code,
        String name,
        Integer credits
) {}


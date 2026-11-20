package kz.edu.sdu.advisor.model.dto;

/**
 * Response payload when loading a saved plan draft into the Planner workspace (US-22).
 */
public record LoadedPlanDraftDto(
        PlanDraftDto draft,
        CartDto cart,
        RoadmapBoardDto roadmap
) {
}

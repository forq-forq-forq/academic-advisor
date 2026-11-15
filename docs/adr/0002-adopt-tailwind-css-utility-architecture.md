# ADR-0002: Adopt Local Tailwind CSS Utility Architecture

- **Date**: 2026-11-15
- **Status**: Accepted

## Context

Templates previously duplicated bespoke CSS rules inline across every HTML view (`T-07` extracted them into static stylesheets). Adding new onboarding chips (`US-03`), password-reset screens (`US-05`), registration banners (`US-09`), and recommendation cards (`US-10`) needed consistent spacing, colors, flex/grid alignment, and typography tokens without depending on external CDN scripts in offline/university-intranet or test environments.

## Decision

Provide a local, deterministic Tailwind v3 utility layer ([tailwind-utilities.css](../../src/main/resources/static/css/tailwind-utilities.css)) and design token manifest ([tailwind-config.js](../../src/main/resources/static/js/tailwind-config.js)) served directly from Spring Boot static resources and loaded across all Thymeleaf views after `common.css`.

## Consequences

- **Positive**: Zero external network dependency during local development, Docker offline builds, or MockMvc CI tests; consistent visual tokens (`slate`, `indigo`, `amber`, `emerald`) across every screen; additive alongside page CSS without breaking legacy selectors or test assertions.
- **Negative**: Utility subset covers classes used by the application rather than the full upstream Tailwind catalog; newly adopted upstream classes must either already exist in `tailwind-utilities.css` or be added there cleanly.

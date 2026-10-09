# US-14: Maximum Credit Limit Warning for Semester Cart

## User Story

**As a** student,  
**I want** a clear warning when my semester cart exceeds the university's maximum credit limit,  
**so that** I am immediately aware that my planned credits exceed the allowable semester registration threshold and may require special approval from an academic advisor or registration adjustments.

---

## Acceptance Criteria

- [x] **Credit Limit Evaluation:** The system evaluates total credits against the configured maximum credit limit (`maxCredits`, default 40 ECTS) whenever the cart is retrieved or modified.
- [x] **Over-Limit Warning Banner:** When total credits exceed `maxCredits`, a prominent warning alert (`#credit-limit-warning`) is displayed in the cart interface specifying total credits, maximum allowable credits, and excess credits (e.g. `"Credit limit exceeded: You have selected 42 ECTS, which exceeds the maximum limit of 40 ECTS. Special advisor permission required."`).
- [x] **Visual Counter & Progress Indicator:** When the limit is exceeded, the credit counter badge updates to a high-contrast danger state (`.credit-limit-exceeded`), and the workload balance progress bar reflects the overload state (100% fill, red color).
- [x] **Real-Time Dynamic Updates:** When adding or removing courses via AJAX, the over-limit warning appears or disappears immediately without requiring a full page refresh.
- [x] **API / DTO Representation:** `CartDto` includes `exceedsLimit` (boolean), `excessCredits` (integer), and `limitWarning` (string) for consistent client-server contract and automated testing.

---

## QA Test

- **Scenario 1:** Given a cart with 38 ECTS and a max limit of 40 ECTS, when a student adds a 4 ECTS course (total 42 ECTS), then the cart shows the warning banner indicating that the 40 ECTS limit is exceeded by 2 ECTS.
- **Scenario 2:** Given a cart exceeding the credit limit (42 / 40 ECTS), when the student removes a 4 ECTS course (total 38 ECTS), then the warning banner automatically disappears and the credit counter reverts to normal styling.
- **Scenario 3:** Given a cart within limits (<= 40 ECTS), when viewing the planner page, then no credit limit warning is displayed.
- **Scenario 4:** Given a call to `CartService.getCart(studentId)` where total credits > maxCredits, then `CartDto.exceedsLimit()` returns `true`, `excessCredits()` returns `totalCredits - maxCredits`, and `limitWarning()` contains descriptive text.

---

## Story Points

2

---

## UI Reference

No response

---

## Additional Notes

- **Configuration:** University maximum credits threshold is controlled via `advisor.cart.max-credits` in `application.properties`.
- **Policy:** In accordance with academic guidelines, this story implements an explicit advisory warning rather than strict blocking, allowing students to draft ambitious schedules while alerting them to registration constraints.


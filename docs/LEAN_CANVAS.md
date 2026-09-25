# Lean Canvas — Academic Advisor

> **Product:** Academic Advisor — AI-Powered Course Planning Platform for SDU University
> **Date:** 2026-09-25
> **Iteration:** v1 (Initial Canvas)
> **Author:** Team forq-forq-forq
> **Methodology:** LEANSTACK Lean Canvas (Ash Maurya)
> **Fill Order:** 2 → 4 → 3 → 9 → 1 → 8 → 5 → 7 → 6 (as recommended by LEANSTACK)

---

## Canvas Overview

```
┌──────────────────┬──────────────┬──────────────────┬──────────────┬──────────────────┐
│    PROBLEM       │   SOLUTION   │  UNIQUE VALUE    │   UNFAIR     │   CUSTOMER       │
│                  │              │  PROPOSITION     │  ADVANTAGE   │   SEGMENTS       │
│  1. Cognitive    │ 1. AI-gen    │                  │              │                  │
│     overload     │    balanced  │  "One click —    │  SDU-native  │  🟡 SDU under-   │
│     at course    │    semester  │   a perfect      │  curriculum  │     grad students │
│     registration │    plans     │   semester.      │  data nobody │     (CS, IS, SE) │
│                  │              │   No burnout,    │  else has"   │                  │
│  2. No 4-year    │ 2. Visual    │   no mistakes,   │              │  🔵 Faculty      │
│     strategic    │    4-year    │   no stress."    │              │     advisors     │
│     visibility   │    roadmap   │                  │              │     (curators)   │
│                  │              │ ──────────────── │              │                  │
│  3. Area elec-   │ 3. Career-   │  HIGH-LEVEL      │              │ EARLY ADOPTERS   │
│     tive         │    driven    │  CONCEPT         │              │                  │
│     paralysis    │    elective  │                  │              │  2nd-year CS     │
│                  │    matching  │  "ChatGPT that   │              │  students facing │
│ EXISTING         │              │   actually knows │              │  first elective  │
│ ALTERNATIVES     │              │   YOUR uni"      │              │  choices         │
│                  │              │                  │              │                  │
│ • Ask classmates │              │                  │              │                  │
│ • 200-page PDFs  │              │                  │              │                  │
│ • Generic ChatGPT│              │                  │              │                  │
│ • Advisor office │              │                  │              │                  │
├──────────────────┼──────────────┼──────────────────┼──────────────┼──────────────────┤
│  KEY METRICS     │              │  CHANNELS        │              │                  │
│                  │              │                  │              │                  │
│ • Plans generated│              │ • Student Tele-  │              │                  │
│ • Time saved     │              │   gram groups    │              │                  │
│ • Prereq errors  │              │ • PMIS defense   │              │                  │
│   prevented      │              │ • Word of mouth  │              │                  │
├──────────────────┴──────────────┴──────────────────┴──────────────┴──────────────────┤
│  COST STRUCTURE                          │  REVENUE STREAMS                          │
│                                          │                                           │
│  • Gemini API tokens (~$5–15/mo)         │  • Free for SDU (academic project)        │
│  • Hosting (VPS ~$5–10/mo)               │  • Future: SaaS license per university    │
│  • Team time (5 devs × 7 sprints)        │  • Future: Freemium for individual users  │
└──────────────────────────────────────────┴───────────────────────────────────────────┘
```

---

## Detailed Breakdown (Block by Block)

### 1. PROBLEM — *Top 3 problems your customers face*

The Lean Canvas starts with problems, not solutions. These three problems were identified through first-hand experience as SDU students and validated through conversations with classmates.

#### Problem 1: Cognitive Overload at Course Registration
> *"I accidentally took 6 hard courses in one semester and barely survived."*

Every semester, students face a list of 30+ available courses and must assemble 5–6 into a coherent schedule. Without workload data, they often stack heavy courses together (Algorithms + Operating Systems + Databases + Networks in one semester), leading to burnout, poor grades, and dropped courses. The credit system technically allows this — no system prevents it.

#### Problem 2: No Strategic 4-Year Visibility
> *"I didn't realize I could have taken 4th-year courses early to free up time for my internship."*

The ECTS credit system gives students flexibility to take courses from any year (if prerequisites are met), but no tool helps them think strategically across all 8 semesters. Students plan one semester at a time, missing opportunities to front-load easy courses, fast-track their degree, or balance workload across years. The 200-page curriculum handbook exists, but nobody reads it cover-to-cover.

#### Problem 3: Area Elective Paralysis
> *"On 3rd year, they gave me a list of 15 electives and I had no idea which ones actually matter for my career."*

Starting from the 3rd year, students must choose Area Electives — specialized courses within their major. The list is long, descriptions are vague, and there's no mapping between electives and career outcomes. Students pick based on rumors ("this professor is easy") rather than strategic career alignment.

#### Existing Alternatives (How these problems are solved today)

| Alternative | Why It Fails |
|---|---|
| **Asking upperclassmen / friends** | Anecdotal, biased, different career goals. "Take course X, it's chill" doesn't account for YOUR transcript. |
| **Reading the 200-page curriculum handbook** | Nobody does this. Dense PDF, no personalization, no interactivity. |
| **Generic ChatGPT / AI chatbots** | Doesn't know SDU curriculum, prerequisite chains, credit limits, or the student's personal transcript. Hallucinates course codes. |
| **Faculty advisor office hours** | 15-minute slots, limited availability, cannot simulate multiple scenarios. Advisor sees 50+ students and cannot remember each plan. |
| **University portal (registrar system)** | Shows available courses but provides zero intelligence — no workload analysis, no prerequisite warnings, no recommendations. |

---

### 2. SOLUTION — *Outline a possible solution for each problem*

Each solution maps directly to one of the three problems. This is the core of the [User Story Map](file:///home/mango59/projects/university/academic-advisor/advisor/docs/USER_STORY_MAP.md).

| Problem | Solution | Release |
|---|---|---|
| **P1: Cognitive overload** | **AI-generated workload-balanced semester plans.** The student clicks one button, and Gemini AI assembles a set of 5–6 courses that respects credit limits (≤30 ECTS), prerequisite chains, and workload balance (max 2–3 heavy courses). Each recommendation comes with a "Why this course?" explanation. | MVP |
| **P2: No 4-year visibility** | **Interactive visual 4-year roadmap with pacing strategies.** A drag-and-drop board showing all 8 semesters. Students can choose "Balanced" or "Fast-track (light 4th year)" strategies. Real-time validation highlights prerequisite violations and overloaded semesters. | v1.0 |
| **P3: Area elective paralysis** | **Career-driven elective matching.** During onboarding, the student describes their career goals ("I want to be a backend developer"). AI filters and ranks Area Electives by relevance to that career path, showing which technologies each course covers. | v1.0 |

---

### 3. UNIQUE VALUE PROPOSITION — *Single, clear, compelling message*

> **"One click — a perfect semester. No burnout, no mistakes, no stress."**

#### Why this works:
- **"One click"** — emphasizes zero effort (vs hours of manual planning).
- **"Perfect semester"** — the outcome the student actually wants.
- **"No burnout, no mistakes, no stress"** — addresses all three emotional pain points.

#### High-Level Concept (X for Y analogy)

> **"ChatGPT that actually knows your university."**

This instantly communicates the differentiator: unlike generic AI, this system has your transcript, your curriculum, your prerequisites, and your university's rules baked in.

Alternative formulations for different audiences:
- For students: *"Your personal academic advisor, available 24/7"*
- For faculty: *"AI that helps students plan smarter — and sends you the plan for approval"*
- For university IT: *"Notion AI meets student information systems"*

---

### 4. UNFAIR ADVANTAGE — *Something that cannot easily be bought or copied*

> [!IMPORTANT]
> Unfair advantages are the hardest box to fill honestly. Most startups leave it blank initially. We have genuine advantages:

| Unfair Advantage | Why It's Hard to Copy |
|---|---|
| **SDU-native curriculum data** | We have digitized the 200-page curriculum handbooks for CS, IS, and SE majors into structured data (courses, prerequisites, credits, semesters, difficulty ratings). A competitor would need to do this from scratch for each university. |
| **Personalized academic context** | The AI doesn't give generic advice — it knows THIS student's exact transcript, GPA, completed courses, and career goals. This context injection is the core technical moat. |
| **First-mover in Kazakh university market** | No similar product exists for SDU or any Kazakh university. Network effects: once students in a major start using it, the community data (course difficulty ratings, elective reviews) becomes self-reinforcing. |
| **Built by students who lived the problem** | The team experienced every pain point first-hand. This translates to product decisions that feel authentic, not manufactured. |

---

### 5. CUSTOMER SEGMENTS — *Target customers and users*

#### Primary Segment: SDU Undergraduate Students

| Attribute | Detail |
|---|---|
| **Who** | Full-time undergraduate students at Suleyman Demirel University |
| **Majors** | Computer Science, Information Systems, Software Engineering (initial scope) |
| **Year** | 1st through 4th year (different needs per year) |
| **Size** | ~500–700 students across 3 majors; ~2,000+ across all Engineering faculty |
| **Key behavior** | Plans courses 2x per year (Fall and Spring registration). Peak usage: 1–2 weeks before registration opens. |

#### Secondary Segment: Faculty Advisors (Curators)

| Attribute | Detail |
|---|---|
| **Who** | Academic advisors assigned to student groups (typically 1 advisor per 20–30 students) |
| **Need** | Currently review student plans verbally in office hours. Want a digital tool to review and approve plans asynchronously. |
| **Size** | ~15–20 advisors across Engineering faculty |
| **Release** | v2.0 (Advisor Approval Portal) |

#### Early Adopters — *Who will use it first?*

The product should be designed for and marketed to these specific sub-segments first:

1. **2nd-year CS students** — facing their first meaningful course choices (beyond the fixed 1st-year curriculum). High anxiety, high motivation to find a tool.
2. **Students with GPA < 2.5** — most anxious about course selection. A wrong choice means academic probation. Highest perceived value from workload balancing.
3. **Transfer / mobility students** — need to rebuild their entire 4-year plan from scratch. The roadmap feature is a lifesaver for them.

---

### 6. KEY METRICS — *The key numbers that tell you how your business is doing*

Using a simplified HEART framework (Google) adapted for an academic product:

#### Activation Metrics (Is the product being used?)

| Metric | Definition | Target (MVP) |
|---|---|---|
| **Plans generated** | Number of AI-generated semester plans created per registration period | 50+ plans in first registration cycle |
| **Registration conversion** | % of generated plans where the student copies the course codes for registration | > 30% |
| **Onboarding completion** | % of registered users who complete profile setup (major + transcript) | > 70% |

#### Value Metrics (Is the product delivering value?)

| Metric | Definition | Target |
|---|---|---|
| **Time saved** | Average time to build a semester plan (AI vs manual). Measured via session duration. | < 5 minutes (vs 2–3 hours manual) |
| **Prerequisite violations prevented** | Number of times the system blocked a course due to missing prerequisites | Track and report |
| **Workload balance score** | Average difficulty variance in generated plans (lower = more balanced) | Variance < 1.5 on a 1–5 scale |

#### Engagement Metrics (Are students coming back?)

| Metric | Definition | Target |
|---|---|---|
| **AI chat messages per session** | Average number of follow-up questions after initial plan generation | > 3 messages |
| **Return visits** | % of students who return within 7 days of first visit | > 40% |
| **AI feedback score** | % of positive ratings (👍) on AI responses | > 75% |

---

### 7. CHANNELS — *Your path to customers (inbound or outbound)*

| Channel | Type | Phase | Expected Impact |
|---|---|---|---|
| **PMIS course defense / demo** | Outbound | MVP launch | Initial exposure to faculty and 30+ classmates. Proof of concept. |
| **SDU student Telegram groups** | Inbound | MVP launch | Primary viral channel. CS/SE/IS major group chats have 200–500 members each. A short demo video + link goes viral before registration. |
| **Word of mouth (peer referral)** | Inbound | Post-MVP | Students who saved time tell their friends. Natural network effect within a major. |
| **University IT / Dean's office partnership** | Outbound | v1.0+ | If the product proves value, pitch to SDU IT department for official integration or recommendation. |
| **GitHub / Open Source community** | Inbound | v1.0+ | Open-source the platform. Other universities fork and adapt for their curricula. |

#### Channel Strategy Summary:
- **MVP:** Focus 100% on the organic Telegram channel. Zero budget, maximum authenticity.
- **v1.0:** Add a formal demo to the Dean's office. If adopted, the university itself becomes the distribution channel.
- **v2.0:** Open-source play — let other universities self-serve.

---

### 8. COST STRUCTURE — *Fixed and variable costs*

#### Fixed Costs (one-time or recurring regardless of usage)

| Cost Item | Amount | Frequency | Notes |
|---|---|---|---|
| Development team time | 5 developers × 7 sprints (14 weeks) | One-time | Academic project, no salary cost. Opportunity cost only. |
| VPS hosting (production) | ~\$5–10/month | Monthly | Single small instance is sufficient for MVP (< 100 concurrent users). |
| Domain name (`advisor.sdu.kz` or custom) | ~\$10/year | Annual | Optional. Can use university subdomain for free. |
| Docker Hub (container registry) | Free tier | — | Public images on free plan. |
| GitHub (repository + CI) | Free tier | — | GitHub Free for public/educational repos. |

#### Variable Costs (scale with usage)

| Cost Item | Unit Cost | Estimated Monthly Usage | Monthly Cost |
|---|---|---|---|
| Google Gemini API (input tokens) | ~\$0.075 / 1M tokens | ~2–5M tokens (500 plans × ~5K tokens each) | ~\$0.15–0.40 |
| Google Gemini API (output tokens) | ~\$0.30 / 1M tokens | ~1–3M tokens | ~\$0.30–0.90 |
| **Total Gemini API cost** | | | **~\$0.50–1.50/month** |

> [!NOTE]
> Gemini API costs are extremely low at academic scale. Even at 2,000 students generating 5 plans each per semester, the total API cost would be under \$15/month. This is not a cost barrier.

#### Total Estimated Monthly Cost: **\$6–12/month** (MVP scale)

---

### 9. REVENUE STREAMS — *Sources of revenue*

#### Current Phase: Academic Project (No Revenue)
The product is being built as part of the PMIS course at SDU. There is no revenue goal for the current phase. The "revenue" is:
- Successful course grade (A/A+).
- A working product that team members add to their portfolios.
- Real impact on fellow students' academic planning.

#### Future Scaling Hypothesis (Post-Graduation)

If the product proves value at SDU, here are viable monetization paths:

| Revenue Model | Description | Viability |
|---|---|---|
| **B2B SaaS (per university)** | License the platform to other Kazakh universities (KBTU, NU, KIMEP, ENU). Each university gets a tenant with their own curriculum data. Pricing: \$500–2,000/year per university. | High — solves a universal problem across all ECTS-based universities. |
| **Freemium (per student)** | Free basic plan (1 semester ahead). Premium (\$3–5/semester) unlocks 4-year roadmap, GPA simulator, and syllabus analysis. | Medium — students have low willingness to pay. Works better if university subsidizes. |
| **University IT integration contract** | University pays for custom integration with their SIS (Student Information System) for automatic transcript import and registration export. | High — if product proves ROI (fewer advising office hours, fewer prerequisite violations). |
| **Open-source + support** | Platform is free and open-source. Revenue from paid onboarding, customization, and support contracts for universities. | Medium — viable at scale with 10+ university deployments. |

---

## Risks & Assumptions to Validate

> Based on LEANSTACK's "Risk Iteration Path" (shown in the bottom-left of the canvas template):

| # | Riskiest Assumption | How to Validate | Priority |
|---|---|---|---|
| 1 | Students actually want AI-generated plans (not just a better PDF). | MVP launch → measure Plans Generated metric. If < 20 in first registration cycle, the problem isn't painful enough. | **P0 — Validate first** |
| 2 | Students trust AI recommendations enough to register based on them. | Measure Registration Conversion rate. If students generate plans but don't copy codes → trust issue. | **P0** |
| 3 | Workload/difficulty ratings are accurate enough to be useful. | Compare AI-balanced plans vs student outcomes (grades). Requires 1 semester of data. | **P1** |
| 4 | Students will fill in their transcript manually (MVP). | Measure Onboarding Completion rate. If < 50% → manual entry is too much friction. Need PDF upload (v2.0) sooner. | **P1** |
| 5 | Faculty advisors will adopt the approval portal. | User interviews with 3–5 advisors before building v2.0. | **P2** |

---

## Canvas Validation Checklist

Per Ash Maurya's *Running Lean* methodology, the canvas should be validated in this order:

- [x] **Problem-Solution Fit** — Are these real problems? Do students confirm them? *(Validated: team members are students who experienced all 3 problems.)*
- [ ] **Product-Market Fit** — Does the MVP actually solve Problem 1? *(Validate after MVP launch during next registration period.)*
- [ ] **Scale** — Can the product serve 2,000+ students and expand to other universities? *(Validate after v1.0.)*

# Bookify Documentation

## Source of truth

Read these documents in order:

1. [Project Overview](./project-overview.md) — product vision and users.
2. [MVP Scope](./mvp-scope.md) — included capabilities, exclusions and acceptance criteria.
3. [Architecture Decisions](./architecture-decisions.md) — accepted technical/product decisions.
4. [System Architecture](./system-architecture.md) — runtime components and trust boundaries.
5. [Software Architecture](./software-architecture.md) — module boundaries and implementation patterns.
6. [Database Design](./database-design.md) — entities, constraints and concurrency.
7. [Non-functional Requirements](./non-functional-requirements.md) — measurable quality expectations.
8. [Current State Assessment](./current-state-assessment.md) — gap between the prototype and target design.
9. [Roadmap](./roadmap.md) — delivery sequence.

When documents disagree, accepted ADRs and the most recently approved MVP scope take precedence. Code that contradicts an accepted decision is technical debt, not an implicit architecture change.

## Documentation rules

- Update affected documents in the same change as a material behavior or architecture change.
- Record significant, hard-to-reverse decisions as ADRs.
- Keep diagrams in an editable text format; exported images are secondary artifacts.
- Mark planned behavior as planned and do not describe prototypes as production-ready.
- Do not add AI, microservices or infrastructure components without an accepted decision.

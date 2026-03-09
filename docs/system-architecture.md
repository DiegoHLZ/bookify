# System Architecture

## Overview

Bookify is designed as a **modular monolithic SaaS application** for service-based businesses. The system is built to support multiple businesses within a single platform while keeping their operational data logically isolated.

The architecture prioritizes:

- simplicity for MVP development
- clear separation of responsibilities
- scalability for future growth
- maintainability and clean structure
- cloud-ready deployment

---

## Architectural Style

The system follows a **modular monolith architecture**.

This means that Bookify is developed as a single backend application, but internally organized into well-defined business modules.

### Why this approach was chosen

A modular monolith is the most suitable architecture for the MVP because it provides:

- lower complexity compared to microservices
- easier development and debugging
- simpler deployment
- better maintainability for a small team or solo developer
- clear path for future modular extraction if the product grows

At this stage, microservices are not necessary because the domain and traffic scale do not justify distributed system complexity.

---

## High-Level Architecture

Bookify is composed of three main layers:

1. **Frontend Application**
2. **Backend API**
3. **Database Layer**

### General Flow

```text
[ Angular Frontend ]
        |
        | HTTPS / REST API + JWT
        v
[ Spring Boot Backend ]
        |
        | JPA / Hibernate
        v
[ PostgreSQL Database ]

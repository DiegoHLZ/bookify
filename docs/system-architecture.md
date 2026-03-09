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

```

---

## Main Components

### Frontend

The frontend is built as a **Single Page Application (SPA)** using Angular.

Responsibilities include:

- rendering the user interface
- handling navigation and routing
- managing forms and validations
- communicating with the backend API
- displaying services, slots and bookings

The frontend contains different areas for:

- public users
- authenticated clients
- business administrators

---

### Backend

The backend is implemented with **Spring Boot** and exposes a REST API.

Responsibilities include:

- authentication and authorization
- business logic execution
- booking validation
- availability slot management
- data persistence
- dashboard metrics generation

The backend acts as the central system coordinator.

---

### Database

The system uses **PostgreSQL** as the relational database.

Responsibilities include:

- storing system data
- enforcing referential integrity
- supporting transactional consistency
- preventing inconsistent booking states

A relational database is used because booking systems require strong consistency and relationships between entities.

---

## SaaS Architecture Model

Bookify follows a **multi-tenant SaaS architecture**.

### Tenant Isolation Strategy

The system uses a **shared database with logical tenant isolation**.

Each business is treated as a tenant within the platform.

Data is isolated using a `business_id` reference.

Examples:

- services belong to a business
- availability slots belong to a business
- bookings belong to a business
- administrators belong to a business

### Why this approach

This model provides:

- simple implementation
- low infrastructure cost
- easy scaling for MVP
- easier maintenance

In the future, the system could evolve to:

- schema-per-tenant
- database-per-tenant

if required by scale.

---

## Backend Modules

The backend is divided into domain modules.

### Auth Module

Responsible for:

- registration
- login
- JWT token generation
- authentication validation

---

### User Module

Responsible for:

- user profile management
- role management
- business association

---

### Business Module

Responsible for:

- business profile
- business ownership
- business configuration

---

### Service Module

Responsible for:

- service creation
- service update
- service listing
- service activation and deactivation

---

### Availability Module

Responsible for:

- availability slot creation
- slot management
- filtering by service and date

---

### Booking Module

Responsible for:

- booking creation
- booking cancellation
- booking status updates
- booking history
- double booking prevention

---

### Dashboard Module

Responsible for:

- booking metrics
- operational statistics
- business activity summaries

---

## Frontend Functional Areas

The frontend application contains three main areas.

### Public Area

Accessible without authentication.

Includes:

- landing page
- login
- registration

---

### Client Area

Accessible to authenticated clients.

Includes:

- browsing services
- selecting available slots
- booking services
- viewing booking history
- cancelling reservations

---

### Admin Area

Accessible to business administrators.

Includes:

- dashboard
- service management
- availability management
- booking monitoring
- business profile configuration

---

## Communication

The frontend communicates with the backend using **REST APIs**.

### Request Flow

1. The frontend sends an HTTP request.
2. The backend authenticates the request using JWT.
3. Business logic is executed.
4. Data is read or written in PostgreSQL.
5. A JSON response is returned.

All communication between frontend and backend uses **JSON**.

---

## Authentication and Security

Bookify uses **JWT-based stateless authentication**.

### Authentication Flow

1. User registers or logs in.
2. Backend validates credentials.
3. Backend generates a JWT token.
4. Frontend stores the token.
5. Token is sent in requests using the Authorization header.

Example:

Authorization: Bearer <token>

### Roles

The system includes two roles:

- ADMIN
- CLIENT

These roles determine access permissions.

---

## Booking Consistency

Booking systems must prevent scheduling conflicts.

Bookify prevents double bookings through two layers of validation.

### Application-Level Validation

Before creating a booking, the backend verifies:

- the slot exists
- the slot belongs to the correct business
- the slot is still available
- the service is active

### Database-Level Protection

The database ensures that one availability slot can only belong to one active booking.

This guarantees consistency even during concurrent requests.

---

## Deployment Architecture

The system is designed to support **containerized deployment**.

Components include:

- Angular frontend
- Spring Boot backend
- PostgreSQL database

Example deployment structure:

Frontend Container  
Backend Container  
PostgreSQL Database

---

## Scalability Strategy

Although the system starts as a modular monolith, the architecture allows future improvements such as:

- notification services
- payment integration
- Redis caching
- background job processing
- analytics services
- module extraction

This approach allows the system to evolve without unnecessary early complexity.

---

## Architecture Summary

Bookify uses a modular monolithic architecture with Angular on the frontend, Spring Boot on the backend and PostgreSQL as the relational database.

The system is designed as a multi-tenant SaaS platform with logical tenant isolation, JWT-based authentication and slot-based booking control.

This architecture provides a strong foundation for building a scalable, maintainable and production-ready booking platform.

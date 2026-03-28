# Software Architecture

## Overview

The backend of Bookify follows a **layered architecture** combined with several well-known software design patterns.  

This approach separates responsibilities across different layers of the application, improving maintainability, testability and scalability.

The architecture ensures that business logic remains independent from infrastructure concerns such as HTTP handling or database access.

---

## Architectural Layers

The backend is organized into the following layers:

```text
Controller
   ↓
Service
   ↓
Repository
   ↓
Database
```
Each layer has a specific responsibility and communicates only with adjacent layers.

---

## Controller Layer

Controllers are responsible for handling incoming HTTP requests and returning responses to the client.

Responsibilities include:

- defining REST API endpoints
- receiving request data
- validating request input
- invoking service layer operations
- returning JSON responses

Controllers should remain lightweight and should not contain business logic.

Examples of endpoints:

- POST /api/auth/login
- GET /api/services
- POST /api/bookings

---

## Service Layer

The service layer contains the core business logic of the system.

Responsibilities include:

- processing booking requests
- validating availability slots
- enforcing business rules
- coordinating repository operations
- managing transactional operations

Example business rules include:

- preventing double booking of the same slot
- ensuring a service belongs to the correct business
- validating that a service is active before booking

This layer acts as the central logic layer of the application.

---

## Repository Layer

The repository layer handles interactions with the database.

In Bookify, repositories are implemented using **Spring Data JPA**.

Responsibilities include:

- retrieving entities from the database
- persisting new entities
- updating existing records
- querying data using domain-specific queries

Repositories abstract database operations from the service layer.

---

## Domain Entities

Entities represent the core business objects stored in the database.

The primary entities of Bookify include:

- User
- Business
- Service
- AvailabilitySlot
- Booking

Entities are mapped to database tables using JPA annotations.

These entities represent the domain model of the system.

---

## DTO Pattern

Bookify uses **Data Transfer Objects (DTOs)** to transfer data between the API and the backend services.

DTOs are used for:

- incoming API requests
- outgoing API responses

Examples include:

- RegisterRequest
- LoginRequest
- BookingResponse
- ServiceResponse

Benefits of using DTOs:

- prevents exposing internal entity models
- allows flexible API responses
- improves API security and clarity

---

## Mapper Pattern

Mappers are responsible for converting between entities and DTOs.

Typical transformations include:

- Request DTO → Entity
- Entity → Response DTO

This separation ensures that the persistence layer remains independent from the API representation layer.

Mapping may initially be implemented manually and later improved using tools such as **MapStruct**.

---

## Exception Handling

The system uses a centralized exception handling strategy.

A global exception handler ensures that all errors are returned in a consistent format.

Responsibilities include:

- capturing runtime errors
- returning structured error responses
- avoiding stack traces being exposed to clients

Examples of handled errors include:

- resource not found
- booking conflicts
- validation errors
- unauthorized access

---

## Security Architecture

Security is implemented using **Spring Security with JWT authentication**.

The authentication process follows these steps:

1. A user logs in with email and password.
2. The backend validates credentials.
3. A JWT token is generated.
4. The frontend stores the token.
5. Subsequent requests include the token in the Authorization header.

Example header:

Authorization: Bearer <token>

Supported roles include:

- ADMIN
- CLIENT

Each role determines which API endpoints the user can access.

---

## Transaction Management

Booking operations require strong transactional consistency.

The service layer ensures that booking operations execute within a transaction.

This guarantees that:

- bookings are created atomically
- slot availability is verified before reservation
- inconsistent states are prevented

This mechanism is essential in reservation systems to prevent race conditions.

---

## Software Architecture Summary

The Bookify backend follows a layered architecture combined with DTO, repository and service layer patterns.

This design ensures:

- clear separation of responsibilities
- maintainable code structure
- scalable business logic
- safe database interactions
- secure API design

The architecture provides a solid foundation for building and evolving the booking platform.

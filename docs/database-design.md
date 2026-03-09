# Database Design

## Overview

The Bookify platform relies on a relational database to manage businesses, services, availability schedules and bookings.

PostgreSQL is used as the database management system because it provides strong consistency, transactional integrity and powerful relational capabilities.

The database is designed to support a **multi-tenant SaaS architecture**, where multiple businesses operate on the same platform while keeping their data logically isolated.

Tenant isolation is achieved through the use of a `business_id` reference in business-related entities.

---

## Core Entities

The main entities of the system are:

- User
- Business
- Service
- AvailabilitySlot
- Booking

These entities represent the core domain model of the reservation platform.

---

## Entity Descriptions

### User

Represents a platform user.

Users can be either administrators of a business or clients who make reservations.

Main attributes include:

- id
- name
- email
- password
- role
- created_at
- updated_at

Roles supported by the system:

- ADMIN
- CLIENT

Administrators are associated with a business, while clients may interact with multiple businesses through bookings.

---

### Business

Represents a business registered in the platform.

Each business manages its own services, availability slots and bookings.

Main attributes include:

- id
- name
- description
- category
- address
- phone
- email
- created_at
- updated_at

A business acts as the tenant boundary for the system.

---

### Service

Represents a service offered by a business.

Examples include:

- haircut
- medical consultation
- sports field rental
- coworking room reservation

Main attributes include:

- id
- business_id
- name
- description
- duration_minutes
- price
- is_active
- created_at
- updated_at

Services can be deactivated instead of deleted in order to preserve booking history.

---

### AvailabilitySlot

Represents a time slot that is available for booking.

Administrators define availability slots for each service.

Main attributes include:

- id
- business_id
- service_id
- start_time
- end_time
- is_available
- created_at
- updated_at

Slots are used to simplify booking logic and prevent scheduling conflicts.

---

### Booking

Represents a reservation created by a client.

A booking links a client, a service and a specific availability slot.

Main attributes include:

- id
- business_id
- service_id
- client_id
- slot_id
- status
- notes
- created_at
- updated_at

Possible booking statuses include:

- PENDING
- CONFIRMED
- CANCELLED
- COMPLETED

---

## Entity Relationships

The system contains the following relationships:

Business 1 → N Services  
Business 1 → N AvailabilitySlots  
Business 1 → N Bookings  

User 1 → N Bookings  

Service 1 → N AvailabilitySlots  
Service 1 → N Bookings  

AvailabilitySlot 1 → 0..1 Booking

This means that one availability slot can only be reserved once.

---

## Multi-Tenant Data Isolation

To support multiple businesses in the same system, several entities contain a `business_id` field.

Entities that include this field:

- Service
- AvailabilitySlot
- Booking

This ensures that data is logically separated between businesses even though they share the same database.

---

## Booking Conflict Prevention

Preventing double booking is critical in reservation systems.

Bookify prevents scheduling conflicts using two mechanisms.

### Application-Level Validation

Before creating a booking, the backend verifies:

- the slot exists
- the slot belongs to the correct business
- the slot is still available
- the service is active

### Database-Level Protection

The database ensures that a slot cannot have more than one active booking.

This guarantees data consistency even if multiple requests occur simultaneously.

---

## Indexing Strategy

Indexes will be applied to frequently queried fields, including:

- business_id
- service_id
- slot_id
- client_id

Indexes improve performance when retrieving bookings, services and availability slots.

---

## Database Summary

The Bookify database design focuses on maintaining strong relationships between entities while ensuring booking consistency and tenant isolation.

The relational structure supports the core reservation workflow and provides a reliable foundation for future system expansion.

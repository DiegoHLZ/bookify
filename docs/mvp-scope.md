# MVP Scope

## Purpose

The MVP (Minimum Viable Product) of Bookify defines the first functional version of the platform. Its goal is to deliver the core booking flow for service businesses while keeping the architecture simple, scalable and suitable for future iterations.

This version focuses on the essential features required for administrators to manage services and availability, and for clients to browse and create reservations.

---

## MVP Objective

The main objective of the MVP is to validate the core value proposition of Bookify:

- allow service businesses to manage their services and available schedules
- allow clients to register, log in and book services
- prevent double booking conflicts
- provide administrators with basic booking management and simple metrics

---

## In Scope

### 1. Authentication and Authorization

The MVP will include authentication and authorization features for two types of users: administrators and clients.

Included features:

- user registration
- user login
- JWT-based authentication
- role-based access control
- roles: `ADMIN` and `CLIENT`

---

### 2. Business Management

The MVP will support the basic registration and ownership of a business inside the platform.

Included features:

- basic business profile
- business information display
- association between admin user and business

---

### 3. Service Management

Administrators will be able to manage the services offered by their business.

Included features:

- create service
- edit service
- list services
- deactivate service

Each service will contain at least:

- name
- description
- duration
- price
- active status

---

### 4. Availability Management

Administrators will be able to define available booking slots.

Included features:

- create availability slots
- list availability slots
- remove or deactivate availability slots
- filter availability by service and date

The MVP will use **explicit availability slots**, meaning that administrators define concrete time blocks available for reservation.

---

### 5. Booking Management

Clients will be able to reserve services based on available slots.

Included features:

- view available services
- view available slots
- create booking
- cancel booking
- view booking history

Administrators will be able to:

- view all bookings of their business
- change booking status
- monitor booking activity

---

### 6. Booking Conflict Prevention

The MVP must prevent double booking of the same availability slot.

Included rules:

- one slot can only be assigned to one active booking
- the backend must validate slot availability before creating a booking
- the database must enforce consistency to avoid duplicate reservations

---

### 7. Admin Dashboard

Administrators will have access to a basic dashboard with business metrics.

Included metrics:

- total bookings
- bookings by status
- recent bookings
- most requested services

---

## Out of Scope

The following features are intentionally excluded from the MVP and may be considered in future versions:

- online payments
- email notifications
- WhatsApp notifications
- multi-branch support
- staff or employee management
- advanced calendar generation
- recurring availability rules
- rescheduling workflows
- discount coupons
- reviews and ratings
- Google OAuth or social login
- refresh token rotation
- mobile app
- advanced analytics
- file uploads
- public custom booking pages per business domain

---

## Functional Boundaries

To keep the MVP manageable, the following limits are defined:

- the platform will support only two roles: `ADMIN` and `CLIENT`
- availability will be managed through manual slots, not dynamic scheduling rules
- the system will support one business context per administrator in the initial version
- bookings will be made only through the web application
- metrics will remain basic and operational, not analytical

---

## Expected Deliverables of the MVP

By the end of the MVP, Bookify should provide:

- a working Angular frontend
- a secure Spring Boot backend API
- a PostgreSQL database model for bookings
- JWT authentication and role-based authorization
- service management for business administrators
- slot-based booking flow for clients
- basic booking dashboard for administrators
- Dockerized deployment-ready structure

---

## Success Criteria

The MVP will be considered successful if:

- administrators can manage services and availability
- clients can register, log in and create bookings
- bookings are stored correctly in the database
- double booking is prevented
- the admin dashboard shows basic operational information
- the application can be deployed in a cloud environment

---

## MVP Summary

The Bookify MVP is focused on validating the essential booking workflow for service businesses through a modular SaaS architecture. It prioritizes usability, consistency, security and maintainability, while leaving advanced automation and business expansion features for future iterations.

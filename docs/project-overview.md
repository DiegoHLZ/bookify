# Bookify

Bookify is a SaaS booking platform designed for service-based businesses such as barbershops, medical clinics, sports centers, beauty salons and coworking spaces.

The platform allows businesses to manage their services, available schedules and reservations from a centralized administrative panel, while clients can easily browse services and book appointments.

## Project Objective

The objective of Bookify is to build a scalable and modular booking platform that enables multiple businesses to manage their reservations efficiently using a modern architecture based on:

- Angular for the frontend
- Spring Boot for the backend
- PostgreSQL as the database
- JWT for authentication
- Docker for deployment

## System Type

Bookify is a **multi-tenant SaaS application** that allows different businesses to use the same platform to manage their booking operations.

Each business manages its own:

- services
- availability schedules
- bookings
- customers

## System Users

### Business Administrator

The administrator is responsible for managing the business operations inside the platform.

Capabilities include:

- managing services
- configuring availability schedules
- viewing bookings
- updating booking status
- monitoring business metrics

### Client

Clients are users who consume the services offered by the business.

They can:

- register
- log in
- view available services
- check availability
- book services
- cancel bookings
- view booking history

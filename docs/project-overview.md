# Bookify — Project Overview

## Product vision

Bookify is a multi-tenant SaaS platform and discovery marketplace for bookable local businesses. A person can describe what they need, discover suitable nearby places, compare trustworthy information and availability, and complete a reservation in one flow.

Initial business categories may include barbershops, beauty salons, clinics, sports venues, coworking spaces, restaurants and other appointment- or resource-based businesses.

## Value proposition

### For customers

- Search by natural language, category, location, date and preferences.
- Compare distance, rating, services, price information and real availability.
- Reserve without calling or messaging each business.
- Manage upcoming and historical bookings.

### For businesses

- Publish locations and service offerings.
- Configure schedules, resources, capacity and booking policies.
- Manage bookings from one operational panel.
- Gain discovery through conventional and AI-assisted search.

### For the platform

- Operate a reusable booking engine across multiple business categories.
- Rank only eligible, trustworthy results.
- Monetize later through subscriptions, commissions or promoted placement with clear disclosure.

## Product boundaries

Bookify is not a separate custom application for every industry. It provides a common booking kernel with configurable business capabilities:

- **Appointment:** a service assigned to a staff member.
- **Exclusive resource:** a court, room, chair or table reserved for an interval.
- **Capacity-based session:** an activity with a finite number of places.

New booking models require an explicit product and architecture decision. The MVP should validate a small set of categories that fit these models rather than claiming universal support immediately.

## AI-assisted discovery

AI helps understand intent and improve ranking. For example:

> “Find a highly rated football field near Miraflores available tomorrow after 7 p.m.”

The search pipeline combines:

- structured filters such as location radius, category, price and availability;
- semantic relevance from business and offering descriptions;
- distance and verified rating signals;
- an optional reranking stage.

AI does not invent businesses, ratings, prices or availability. Results come from Bookify's indexed records, and reservation creation is always validated by the transactional booking engine.

## Product principles

- Marketplace discovery and transactional booking are separate capabilities.
- The server and database are authoritative for availability and reservations.
- Tenant data is isolated at every business boundary.
- Search ranking is explainable enough to show why a result appears.
- Paid placement, if introduced, is labeled and cannot falsify relevance or availability.
- The core platform remains usable when AI services are unavailable.
- The MVP starts as a modular monolith and evolves based on measured needs.

## Confirmed technology baseline

- Angular web application.
- Spring Boot with Java 17.
- PostgreSQL; PostGIS is recommended for geo-distance queries.
- Spring Security and JWT authentication.
- NVIDIA NeMo Retriever Embedding NIM and Reranking NIM are candidates for semantic retrieval and reranking, behind provider-neutral interfaces.

Exact NVIDIA models, deployment mode and GPU infrastructure require a benchmark and ADR; they are not hardcoded into the booking domain.

## Success outcome

The product succeeds when a customer can discover a relevant nearby business with real availability and complete a conflict-free reservation, while each business can manage only its own operational data.

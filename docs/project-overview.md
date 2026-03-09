# Bookify

Bookify es una plataforma SaaS de gestión de reservas diseñada para negocios de servicios como barberías, consultorios, centros deportivos, salones de belleza o coworkings.

La plataforma permite que los negocios administren sus servicios, horarios disponibles y reservas desde un panel administrativo centralizado, mientras que los clientes pueden consultar disponibilidad y reservar servicios de manera rápida y sencilla.

## Objetivo del proyecto

El objetivo de Bookify es desarrollar una plataforma escalable y modular que permita a múltiples negocios gestionar sus reservas de manera eficiente mediante una arquitectura moderna basada en:

- Angular para el frontend
- Spring Boot para el backend
- PostgreSQL como base de datos
- JWT para autenticación
- Docker para despliegue

## Tipo de sistema

Bookify es una aplicación **SaaS (Software as a Service)** multi-negocio que permite que distintos negocios utilicen la misma plataforma para gestionar sus reservas.

Cada negocio tendrá su propio conjunto de:

- servicios
- horarios disponibles
- reservas
- clientes

## Usuarios del sistema

### Administrador del negocio

Puede:

- gestionar servicios
- configurar horarios disponibles
- ver reservas
- cambiar estados de reservas
- visualizar métricas del negocio

### Cliente

Puede:

- registrarse
- iniciar sesión
- ver servicios disponibles
- consultar horarios
- reservar servicios
- cancelar reservas
- ver historial de reservas

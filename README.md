# Enterprise Integration Service

A Java-based enterprise integration application built with Spring Boot
and Apache Camel. It demonstrates file processing, data transformation,
REST API communication, SFTP transfers, EDIFACT parsing, database-backed
order tracking, failure handling and operational monitoring.

## Business Problem

Business partners exchange orders using different data formats and
communication protocols. Applications must transform this data,
validate it, communicate with destination systems and handle failures
without losing important business information.

This project demonstrates practical solutions to these integration
challenges.

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Application development |
| Spring Boot 4.1.1 | Application framework |
| Apache Camel 4.22.2 | Integration routes |
| MySQL 8.4 | Order persistence and history |
| Spring JDBC | Database access |
| REST / HTTP | API communication |
| SFTP / SSH | Secure file exchange |
| CSV, JSON, XML | Data formats and transformation |
| EDIFACT | Partner-specific ORDERS subset |
| Spring Boot Actuator | Health and metrics |
| Bash | Operational automation |
| Maven | Build and dependency management |

## Architecture

The application contains several integration workflows.

```mermaid
flowchart TD
    A[CSV Files] --> B[Apache Camel]
    B --> C[CSV Parser]
    C --> D[Java Order Model]
    D --> E[JSON / XML Transformation]

    F[CSV / JSON Input] --> G[HTTP Integration Routes]
    G --> H[Local REST API]
    H --> I[HTTP Response / Error Handling]

    J[EDIFACT ORDERS File] --> K[EDIFACT Parser]
    K --> L[OrderTrackingService]
    L --> M[(MySQL)]
    M --> N[Order Status History]

    O[SFTP Server] --> P[SFTP Inbound Route]
    P --> Q[Local Received Files]

    R[Local Outgoing Files] --> S[SFTP Outbound Route]
    S --> O

    T[Actuator / Structured Logs] --> U[Monitoring]
    V[Bash Scripts] --> U

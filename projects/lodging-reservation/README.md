# Lodging Reservation Management System

A Java 21 object-oriented application modeling lodging accounts and hotel, cabin, and house reservations. The application manages account membership, globally unique reservation numbers, validated state transitions, subtype-specific pricing, and local JSON file persistence.

## Design
- `Manager` coordinates accounts and enforces reservation-number uniqueness across managed accounts.
- `Account` owns reservation collections, retrieval, updates, and persistence delegation.
- Abstract `Reservation` implements shared validation and lifecycle state; `HotelReservation`, `CabinReservation`, and `HouseReservation` implement specialized pricing.
- `Address` encapsulates address data; custom exceptions provide domain-specific failure information.

## Run
Requires JDK 21 and Maven. From this directory:

```bash
mvn -q -DskipTests package
mvn -q -Dexec.mainClass=lodgingreservation.Test -Dexec.classpathScope=test test-compile exec:java
```

The second command executes the standalone scenario harness (not JUnit). The original validated baseline reported **635 assertions across 26 scenarios**. Re-run on this reorganized Maven layout before claiming that same result for the portfolio version.

## Persistence
The manager reads account manifests from a local directory. Each account stores its own manifest and reservation JSON files; Gson 2.13.2 handles JSON. No database, hosted service, or user interface is included.

## Scope
This is an educational-origin engineering case study reorganized for portfolio review, not a production-deployed booking service. Design decisions, limitations, and verification evidence are described in the project documentation.

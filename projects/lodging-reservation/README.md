# Lodging Reservation Management System

**Java 21 · Maven · Gson 2.13.2 · Object-Oriented Programming · JSON Persistence**

## Overview
A Java domain application for managing lodging accounts and reservations for hotels, cabins, and houses. It models account ownership, reservation-number uniqueness, subtype-specific nightly pricing, permitted lifecycle transitions, and persistence through local JSON files. No GUI, web server, payment system, or database is included.

## Architecture

```text
Manager
  └── Account
        └── Reservation (abstract)
              ├── HotelReservation
              ├── CabinReservation
              └── HouseReservation
Address — account/reservation address value
JsonData + Gson — JSON parsing and file support
Custom exceptions — invalid loads, duplicates, and operations
```

**Responsibilities:** `Manager` coordinates accounts and enforces uniqueness across managed accounts; `Account` handles its reservation collection and file delegation; abstract `Reservation` handles common validation, state, and total-price logic; subclasses override nightly pricing.

## Capabilities
- Create accounts, add reservations, retrieve accounts and reservations.
- Reject duplicate reservation numbers across managed accounts, including case-insensitive duplicates.
- Complete eligible draft reservations and store the calculated total.
- Cancel eligible future draft reservations and reject invalid final-state transitions.
- Update permitted draft reservations while preserving identity and ownership.
- Serialize and restore account manifests and reservation records as UTF-8 JSON.
- Throw custom domain exceptions for invalid operations, duplicate objects, and load failures.

## Pricing model
| Lodging | Nightly calculation |
|---|---|
| Common | $120 base; add $15 when lodging size exceeds 900 sq ft |
| Hotel | Common + $50; add $10 when kitchenette is present |
| Cabin | Common + $20 for full kitchen; add $5 for each bathroom beyond the first |
| House | Common rule; no additional floor surcharge |

Total price is nightly price multiplied by number of nights.

## Build and verification

Requires **JDK 21** and **Maven**. From this directory:

```bash
mvn -B -ntp -DskipTests package
mvn -B -ntp -Dexec.mainClass=lodgingreservation.Test -Dexec.classpathScope=test test-compile exec:java
```

The project includes a standalone Java scenario harness at `src/test/java/lodgingreservation/Test.java`; it is **not JUnit**.

**Verified on GitHub Actions:** JDK 21 Maven build and scenario harness passed **635 checks across 26 scenarios**, with the actual Maven-resolved Gson 2.13.2 JAR verified by hash. The verification was performed on the curated portfolio layout (not merely the original project).

## Source navigation
- [Production Java classes](src/main/java/lodgingreservation/)
- [Scenario test harness](src/test/java/lodgingreservation/Test.java)
- [Maven project configuration](pom.xml)

## Design and operational limitations
This is a local-file Java domain model, not a deployed booking platform. Multi-file persistence is not an account-wide atomic transaction. The repository is an educational-origin case study curated for technical evaluation.

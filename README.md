# Software Design & Implementation

**Java applications · Object-oriented architecture · UML system modeling · Verification**

A collection of engineering case studies illustrating how functional requirements become object models, executable Java code, persistence mechanisms, and verifiable behavior. The focus is **design decisions and correctness**, not screenshots or claims of production deployment.

> **Portfolio status:** The reorganized Java 21 Maven application has passed its portfolio-layout build and 635 scenario assertions; Art Management's native UML model and SVG are now available; a refreshed PNG export is pending. The Art Management model is documented, with an editable native UML model and complete vector diagram available.

## Explore the projects

| Case study | What it demonstrates | Deliverables | Status |
|---|---|---|---|
| **[Lodging Reservation Management](projects/lodging-reservation/)** | Domain modeling, reservation lifecycle, global uniqueness, polymorphic pricing, JSON persistence, error handling | Java 21 source, Maven configuration, standalone scenario tests, technical README | **Implemented and verified** on GitHub Actions |
| **[Art Inventory & Transaction Management](projects/art-management/)** | UML analysis, abstract art hierarchy, subtype-specific shipping, customer/transaction relationships, transaction integrity | Design overview; native StarUML model and diagrams pending transfer | **Design only** |

## Case study 1 — Lodging Reservation Management

### Problem and scope
Model lodging accounts and hotel, cabin, and house reservations while keeping business rules inside domain objects rather than scattering them across callers. The application is a **Java domain system**, not a web booking site.

### Architecture at a glance

```text
Manager
  └── Account (multiple)
        └── Reservation (abstract; multiple)
              ├── HotelReservation
              ├── CabinReservation
              └── HouseReservation

Address → used by Account and Reservation
JsonData + Gson → JSON parsing / serialization
Domain exceptions → validation and operation failures
```

- **Manager:** account coordination, lookup, and reservation-number uniqueness across managed accounts.
- **Account:** reservation membership, retrieval, controlled updates, and persistence delegation.
- **Reservation:** shared identity, attributes, lifecycle constraints, and total-price calculation.
- **Concrete reservation types:** specialized nightly-price calculations via overriding.
- **Address and JSON helpers:** value representation and file-format handling without requiring a database.

### Supported behavior
Create and manage accounts; add and retrieve reservations; enforce globally unique reservation numbers within a manager; validate eligible draft-to-completed and draft-to-cancelled transitions; update permitted draft reservations; calculate nightly and total prices; and save/load JSON data from local files. Custom exceptions communicate invalid operations and duplicate identifiers.

**Implementation boundaries:** No graphical interface, payment processing, network service, or production database is included. File persistence is not an account-wide database transaction.

### Engineering evidence
The reorganized Maven project **passed 635 assertions across 26 scenarios** on GitHub Actions using Java 21 and the real Maven-resolved Gson 2.13.2 JAR. The test also verifies the JAR's SHA-256 identity.

Explore: **[Source, test harness, setup, and domain details →](projects/lodging-reservation/)**

## Case study 2 — Art Inventory & Transaction Management

### Problem and scope
Design an object model for artwork inventory, customers, and transactions. The design considers art identity, shipping costs, transaction state, and inventory availability without pretending that a UML model is executable software.

### Architecture at a glance

```text
ArtManager → inventory and transaction coordination
Art (abstract)
  ├── Painting
  ├── Drawing
  ├── Print
  └── Sculpture
Transaction → Customer and selected Art
Custom exceptions → invalid art and transaction operations
```

The four concrete art classes override a shipping-cost operation. Constructor overloading demonstrates alternative initialization paths. Relationships, multiplicities, and encapsulation are modeled explicitly. Transaction completion is designed to verify availability before inventory mutation.

**Deliverable boundary:** This is a **StarUML design case study**. No Java runtime implementation or automated runtime tests are claimed. The native model and full SVG are available in the project directory. A refreshed PNG export from the portfolio-specific model is pending.

Explore: **[Art Management design overview →](projects/art-management/)**

## Methods and technologies

| Area | Evidence in this portfolio |
|---|---|
| Object-oriented analysis | Responsibilities, class boundaries, domain invariants |
| Modeling | UML classes, inheritance, aggregation, multiplicity, abstract operations |
| Java implementation | Java 21, constructors, inheritance, encapsulation, overriding |
| Persistence | Gson 2.13.2, local UTF-8 JSON manifests and reservation records |
| Error handling | Input validation and custom domain exceptions |
| Verification | Scenario-based Java assertions, strict compilation in the original baseline |
| Version control | Curated GitHub source and documented engineering decisions |

## Run the Java case study

**Requirements:** JDK 21 and Maven. From `projects/lodging-reservation/`:

```bash
mvn -q -DskipTests package
mvn -q -Dexec.mainClass=lodgingreservation.Test -Dexec.classpathScope=test test-compile exec:java
```

The `Test` class is a standalone scenario harness, not a JUnit suite. See the [project README](projects/lodging-reservation/README.md) for details. The reorganized Maven build and scenario harness have passed on GitHub Actions.

## How to evaluate this portfolio

Start with the [Lodging Reservation source](projects/lodging-reservation/src/main/java/lodgingreservation/) and [scenario tests](projects/lodging-reservation/src/test/java/lodgingreservation/Test.java) to inspect implemented behavior. Use the [Art Management overview](projects/art-management/) to assess the UML modeling approach and design decisions.

These case studies were initially developed in an educational context and later curated for technical review. They are **not commercial client engagements, deployed services, or certified production systems**.

## Current development status

The curated import is under review on the `portfolio-project-import` branch. The original academic archive remains private. Public-facing content should be judged by the actual files and verification evidence present here; The Art Management native model and SVG are available, while a refreshed PNG export remains pending; the Java Maven build is verified.

# Art Inventory & Transaction Management — UML Design

**StarUML · Object-Oriented Analysis · UML Class Modeling**

A design case study for an art inventory and transaction system. The model specifies art identity and specialized shipping calculations, customer data, inventory availability, and transaction completion.

## Domain model

- `ArtManager` coordinates art inventory and transactions.
- Abstract `Art` defines shared art properties and shipping behavior.
- `Painting`, `Drawing`, `Print`, and `Sculpture` specialize art data and override shipping calculation.
- `Customer` represents customer details.
- `Transaction` models selected art, customer association, and completion state.
- Three custom exception classes represent invalid art and transaction operations.

## Object-oriented techniques
Inheritance, abstraction, overriding, constructor overloading, encapsulation, aggregation, two-ended multiplicity, and custom exceptions. The model's transaction completion contract requires availability checks before any inventory removal.

## Current deliverables
This is a **design-only** case study. Native StarUML `.mdj` and final diagram exports are pending a publication-safety review and are not yet present in this public repository. No Java implementation or runtime tests are claimed.

## Scope
No GUI, payment processing, database, or commercial deployment is claimed. The design originated in an educational context and is presented as a UML modeling example.

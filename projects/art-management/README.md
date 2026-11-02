# Art Inventory & Transaction Management

**Object-oriented analysis · UML · StarUML · Domain modeling**

> **Project type:** Design case study. The repository contains an editable native StarUML model and a complete SVG class diagram. No Java implementation is claimed.

## Problem

Model a small art inventory and transaction workflow with explicit rules for artwork types, shipping costs, customer data, transaction completion, and inventory availability. The objective is to express a maintainable object-oriented design, not to build a commercial storefront.

## Model overview

```text
ArtManager — coordinates inventory, customer-related transactions, and local persistence
Art (abstract)
 ├── Painting
 ├── Drawing
 ├── Print
 └── Sculpture
Transaction — customer + selected artworks; controlled completion
Customer — customer information
InvalidArtOperationException
InvalidTransOperationException
InvalidTransactionException
```

The three custom exceptions specialize Java's `RuntimeException`.

## Responsibilities and design decisions

- **ArtManager** manages available art, transaction records, retrieval, and transaction completion orchestration.
- **Art** defines common immutable identity and descriptive properties and an abstract `calculateShippingCost(): double` operation.
- **Concrete art classes** override shipping-cost calculation with subtype-specific rules.
- **Transaction** retains a customer and selected art; the manager verifies inventory availability before a transaction is finalized and artworks are removed.
- **Customer** stores customer contact data independently of an individual transaction.
- **Relationships** use shared aggregation with multiplicities rather than inventing exclusive object-lifetime ownership.
- **Encapsulation** protects art identity, transaction identity, and completed transaction data.

### Polymorphism

**Overriding:** `Painting`, `Drawing`, `Print`, and `Sculpture` each override `calculateShippingCost(): double`.

**Constructor overloading:** The native model includes `ArtManager()` and `ArtManager(inventoryFile: String, transactionsFile: String)`, plus two `Transaction` constructors with different parameter lists. These are explicit overloads, not overrides.

## Design artifacts

- [Native StarUML model](model/art-management.mdj) — editable UML project.
- [Complete class diagram (PNG)](docs/class-diagram.png) — high-resolution raster rendering of the reviewed SVG.
- [Complete class diagram (SVG)](docs/class-diagram.svg) — scalable vector rendering for detailed inspection.

The native model was imported from the previously reviewed design and its diagram title was made portfolio-neutral. **It has not yet been reopened and re-exported in StarUML after this portfolio-specific edit.** The SVG is the previously validated native export. The PNG is rendered from the reviewed StarUML-exported SVG using CairoSVG, **not exported directly from the portfolio-edited MDJ**. A native StarUML reopen/export of the portfolio MDJ remains an optional consistency check.

## Verification scope

The original design review reported 12 displayed classes (11 project classes plus Java RuntimeException), 7 inheritance connectors, 4 shared aggregations, 39 attributes, 83 UML operations including 13 constructors, and 3 custom exceptions. These figures describe the prior validated model. No executable Java behavior or runtime test coverage is claimed.

## Limitations

This is a static UML design study. There is no application UI, persistence implementation, payment integration, or database. Transaction atomicity and shipping calculations are design contracts, not demonstrated runtime behavior. The project originated in an educational setting and was curated for technical review.

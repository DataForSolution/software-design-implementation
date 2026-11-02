# Portfolio publication verification

## Current public repository
This repository is public and uses new Git history. Its source archive remains private. The `portfolio-project-import` branch is a staged public portfolio, not yet merged into `main`.

## Imported artifacts
- Final LodgingReservation Java 21 source and standalone scenario harness.
- Maven dependency declaration for Gson 2.13.2; no third-party binary JAR committed.
- Project-specific READMEs and professional root documentation.
- Art Management project overview only; native diagrams and model are pending review.

## Verified Java build
GitHub Actions run **41481705551** completed successfully on commit `e4cae22855036d7e5c075275e44f85bc09dd4884`.
- Maven production compilation: PASS.
- Standalone `lodgingreservation.Test`: **635 checks passed; 26/26 scenarios passed**.
- Gson loaded from Maven classpath, real JAR identity and SHA-256 verified.
- Prior initial CI failure was caused by the old test assuming a local `lib/gson-2.13.2.jar`; corrected to resolve the actual Maven classpath location, without weakening the hash check.

## Remaining release gates
- Review and transfer Art Management native StarUML model/diagram only if redistribution is appropriate.
- Inspect complete public files for PII, school handouts, feedback, private paths and licensing concerns.
- Confirm README links and accurate status.
- Promote the curated branch to main only after review and approval.

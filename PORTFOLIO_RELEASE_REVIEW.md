# Public portfolio publication audit

## Repository separation
- The historical source repository is **private** and remains separate from this new public, clean-history repository.
- The public repository contains curated Java source, project documentation, an editable Art Management UML model, an SVG diagram, and a PNG rendered from that SVG.
- No assignment handouts, instructor feedback, grades, local Windows paths, historical course ZIPs, or course-specific branch history were intentionally transferred.

## Java project
- Java 21 Maven build and real Gson 2.13.2 dependency: **PASS** in GitHub Actions.
- Scenario harness: **635 assertions / 26 scenarios passed**.
- Updated checkout/setup-java GitHub Actions v5: **PASS** on main at run `41490005632`.
- No packaged third-party binary JAR is committed; Gson is resolved by Maven.

## Art Management project
- Imported final previously reviewed StarUML model with a neutral diagram title.
- Imported complete native-export SVG.
- Documentation distinguishes design contracts from implemented Java behavior.
- Prior model validation reported 644 unique model/view identifiers, 1,212 resolved references, and zero native validator failures.
- **Current imported model not yet reopened/re-exported in StarUML after neutral title edit.** Do not describe the new model as independently native-validated.
- **PNG added:** GitHub Actions run 41493590350 rendered the previously native-exported SVG to a full-resolution PNG and committed it. This is a derived PNG, not a direct StarUML export of the portfolio-edited MDJ.

## Publication and privacy
- This repository is **already public**. Any commits on the working branch are also public.
- Basic text screening of the imported MDJ found only a former diagram name containing the week reference; it was renamed.
- The SVG was copied from the reviewed native export; a comprehensive binary/metadata audit has not been independently rerun here.
- No new license has been assigned to the portfolio. The presence of a public repository does not imply blanket permission to reuse all contents.
- Provenance is described truthfully as educational-origin work, not commercial deployment.

## Remaining release gates
1. Optionally open `projects/art-management/model/art-management.mdj` in licensed StarUML and compare its current rendering to the published SVG/PNG; the diagram-title edit was not independently native-validated.
2. Complete a final public-content inspection, including embedded metadata and any third-party assets.
3. Confirm both project README links and main README statements match actual files.
4. Review PR and GitHub Actions before merging to `main`.

**Status:** Art Management model, SVG, and derived PNG are staged. Final public-content audit and review before merge remain open.

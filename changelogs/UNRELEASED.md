# Unreleased

<!-- Record user-visible changes here as work lands. This draft is never published directly. -->

## Added

- Maven publications for the standalone developer API and loader artifacts, including sources and Javadocs, using the
  iso2t repository.

- Developer API plugin discovery on Fabric and NeoForge, with validated startup registrations and weight-query services.
- API callbacks for committed weight revisions, changed player snapshots, client readiness/disconnects, and server
  shutdown.
- Developer wiki guides for plugin setup, queries, storage and capacity providers, HUD integration, notifications,
  and testing, with examples checked against the standalone API.
- HUD API for moving, hiding, decorating, or replacing the ring and numeric display, with shared XP positioning
  and scoped drawing helpers. Client settings can select a renderer for each element.

- Gameplay API providers for extra inventory slots, custom container contents, and additive capacity bonuses.
  Inventory/container failures produce incomplete weight; invalid capacity contributions add no bonus.

## Changed

- Incomplete weight feedback also covers unavailable custom-container contents; tooltips no longer claim that every
  incomplete preview exceeded a calculation limit.

- Configuration screens now share one entry point and create fresh controls on each open.
- Tooltips calculate per-item and maximum-stack details only when expanded.
- Simplified shared configuration screens and internal state handling without changing encumbrance rules.

- Refreshed item weight tooltips with light gray labels, gold values, and a clearer Shift hint using vanilla text
  colors.
- Weight tooltips show only the hovered stack's total by default (or item weight for a single item). Hold Shift for
  per-item and maximum-stack details where relevant.

## Fixed

## Removed

- Removed unused legacy weight-file APIs, the old item-weight cache, and obsolete configuration-screen wrappers. Legacy
  datapack conversion remains available.

## Compatibility and upgrade notes

- Player-state networking now includes external capacity bonuses. Clients and servers need matching HI builds.

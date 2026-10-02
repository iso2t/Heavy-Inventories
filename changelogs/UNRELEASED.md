# Unreleased

<!-- Record user-visible changes here as work lands. This draft is never published directly. -->

## Added

## Changed

- Configuration screens now share one entry point and create fresh controls on each open.
- Tooltips calculate per-item and maximum-stack details only when expanded.
- Simplified shared configuration screens and internal state handling without changing encumbrance rules.

- Refreshed item weight tooltips with light gray labels, gold values, and a clearer Shift hint using vanilla text
  colors.
- Weight tooltips show only the hovered stack's total by default (or item weight for a single item). Hold Shift for
  per-item and maximum-stack details where relevant.

## Fixed

- HI's tooltips, HUD, feedback, command messages, and config-screen titles and descriptions fall back to English when a translation is missing.

## Removed

- Removed unused legacy weight-file APIs, the old item-weight cache, and obsolete configuration-screen wrappers. Legacy
  datapack conversion remains available.

## Compatibility and upgrade notes

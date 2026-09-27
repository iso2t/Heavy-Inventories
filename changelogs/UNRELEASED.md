# Unreleased

<!-- Record user-visible changes here as work lands. This draft is never published directly. -->

## Added

## Changed

- Simplified shared configuration screens and internal state handling without changing encumbrance rules.

- Refreshed item weight tooltips with light gray labels, gold values, and a clearer Shift hint using vanilla text
  colors.
- Weight tooltips show only the hovered stack's total by default (or item weight for a single item). Hold Shift for
  per-item and maximum-stack details where relevant.

## Fixed

- Opening the legacy configuration API before registering a screen no longer throws a null-pointer exception.

## Removed

## Compatibility and upgrade notes

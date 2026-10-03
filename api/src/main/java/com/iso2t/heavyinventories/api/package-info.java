/**
 * Heavy Inventories integration contracts. All weights are pounds, before display rounding.
 * Server access belongs on the owning server thread; client access belongs on the client thread.
 * Readiness is reported by query results, never by a fabricated zero weight.
 * Client rendering contracts are isolated in {@code api.client} and must not be loaded on a dedicated server.
 */
package com.iso2t.heavyinventories.api;

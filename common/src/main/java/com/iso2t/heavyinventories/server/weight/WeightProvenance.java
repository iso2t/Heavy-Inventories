package com.iso2t.heavyinventories.server.weight;

/** Stored with the active weight table, never reconstructed from possibly rejected reload data. */
public record WeightProvenance(Source source, WeightPackData.Entry definition) {
    public enum Source { EXPLICIT, RECIPE, FALLBACK, SESSION }
    public static final WeightProvenance FALLBACK = new WeightProvenance(Source.FALLBACK, null);
    public static final WeightProvenance SESSION = new WeightProvenance(Source.SESSION, null);
}

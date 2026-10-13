package com.orchestration.backend.tracing;

import java.util.function.Supplier;

/**
 * Accumulateur de temps par couche pour la requete en cours, tenu dans un
 * ThreadLocal (un thread Tomcat traite une requete a la fois). Pose par
 * TraceTimingFilter en debut de requete, alimente par les services metier
 * (timeDb/timeBusiness), lu par le filtre en fin de requete pour construire
 * l'en-tete Server-Timing et la ligne persistee dans request_traces.
 */
public class TraceContext {

    private static final ThreadLocal<TraceContext> CURRENT = new ThreadLocal<>();

    private final String traceId;
    private final long startNanos;
    private long dbNanos = 0;
    private long businessNanos = 0;

    private TraceContext(String traceId) {
        this.traceId = traceId;
        this.startNanos = System.nanoTime();
    }

    public static TraceContext start(String traceId) {
        TraceContext ctx = new TraceContext(traceId);
        CURRENT.set(ctx);
        return ctx;
    }

    public static TraceContext current() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public <T> T timeDb(Supplier<T> op) {
        long start = System.nanoTime();
        try {
            return op.get();
        } finally {
            dbNanos += System.nanoTime() - start;
        }
    }

    public void timeDb(Runnable op) {
        timeDb(() -> {
            op.run();
            return null;
        });
    }

    public <T> T timeBusiness(Supplier<T> op) {
        long start = System.nanoTime();
        try {
            return op.get();
        } finally {
            businessNanos += System.nanoTime() - start;
        }
    }

    public String getTraceId() {
        return traceId;
    }

    public double getDbMs() {
        return dbNanos / 1_000_000.0;
    }

    public double getBusinessMs() {
        return businessNanos / 1_000_000.0;
    }

    public double getElapsedMs() {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }
}

package com.orchestration.backend.tracing;

import com.orchestration.backend.entity.RequestTrace;
import com.orchestration.backend.repository.RequestTraceRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

/**
 * Etablit le TraceContext pour la requete, propage l'identifiant de trace
 * (recu du frontend via X-Trace-Id, ou genere si absent), et a la fin de la
 * requete construit l'en-tete Server-Timing (visible dans l'onglet Reseau du
 * navigateur) et persiste la ligne de trace pour les requetes significatives.
 */
@Component
@RequiredArgsConstructor
public class TraceTimingFilter extends OncePerRequestFilter {

    private static final String TRACE_HEADER = "X-Trace-Id";

    private final RequestTraceRepository requestTraceRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        TraceContext ctx = TraceContext.start(traceId);

        // La reponse est bufferisee : sans ca, un corps en Transfer-Encoding: chunked
        // committe les en-tetes des la premiere ecriture, avant qu'on connaisse les
        // durees (calculees apres le traitement de la requete).
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        wrappedResponse.setHeader(TRACE_HEADER, traceId);

        try {
            filterChain.doFilter(request, wrappedResponse);
        } finally {
            double totalMs = ctx.getElapsedMs();
            double dbMs = ctx.getDbMs();
            double businessMs = ctx.getBusinessMs();
            double apiMs = Math.max(0, totalMs - dbMs - businessMs);

            wrappedResponse.setHeader("Server-Timing", String.format(Locale.ROOT,
                    "api;dur=%.2f, business;dur=%.2f, db;dur=%.2f, total;dur=%.2f",
                    apiMs, businessMs, dbMs, totalMs));

            if (isSignificant(request)) {
                requestTraceRepository.save(RequestTrace.builder()
                        .traceId(traceId)
                        .endpoint(request.getMethod() + " " + request.getRequestURI())
                        .apiMs(apiMs)
                        .businessMs(businessMs)
                        .dbMs(dbMs)
                        .totalMs(totalMs)
                        .build());
            }

            wrappedResponse.copyBodyToResponse();
            TraceContext.clear();
        }
    }

    private boolean isSignificant(HttpServletRequest request) {
        return "POST".equals(request.getMethod()) && request.getRequestURI().equals("/api/v1/tasks");
    }
}

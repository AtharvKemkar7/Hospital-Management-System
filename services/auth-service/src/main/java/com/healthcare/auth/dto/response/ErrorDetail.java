package com.healthcare.auth.dto.response;

/**
 * Per-field error detail. Mirrors the foundation's error envelope.
 */
public record ErrorDetail(String field, String issue) { }

package com.example.gateway.resource;

public record GatewayErrorResponse(int status, String code, String message, String correlationId) {}

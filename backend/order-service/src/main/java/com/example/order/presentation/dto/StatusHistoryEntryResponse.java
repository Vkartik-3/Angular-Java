package com.example.order.presentation.dto;

import java.time.Instant;

public record StatusHistoryEntryResponse(String status, Instant occurredAt) {}

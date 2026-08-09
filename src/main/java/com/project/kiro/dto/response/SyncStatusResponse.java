package com.project.kiro.dto.response;

import java.time.Instant;

public record SyncStatusResponse(Instant lastSyncAt, int importedCount) {}

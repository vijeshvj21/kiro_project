package com.project.kiro.service;

import java.time.LocalDate;

public record EmailContent(String body, String subject, LocalDate receivedDate) {}

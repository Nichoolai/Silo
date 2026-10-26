package com.silo.model;

import java.time.LocalDate;

/** Quantity of one donation delivered to one recipient. */
public record Distribution(int id, int donationId, int recipientId, double quantity, LocalDate deliveredOn) { }

package com.silo.model;

import java.time.LocalDateTime;

public class Pickup {
    public enum Status { SCHEDULED, PICKED_UP, DELIVERED, CANCELLED }

    private int id, donationId, volunteerId;
    private LocalDateTime scheduledAt;
    private Status status = Status.SCHEDULED;

    public Pickup(int id, int donationId, int volunteerId, LocalDateTime scheduledAt) {
        this.id = id; this.donationId = donationId; this.volunteerId = volunteerId; this.scheduledAt = scheduledAt;
    }
    public int getId() { return id; }
    public int getDonationId() { return donationId; }
    public int getVolunteerId() { return volunteerId; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status s) { status = s; }
    public void reschedule(int volunteerId, LocalDateTime when) { this.volunteerId = volunteerId; this.scheduledAt = when; }
}

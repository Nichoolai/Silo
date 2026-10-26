package com.silo.model;

import java.time.LocalDate;

/** A surplus-food donation. Perishability: 1 (stable) to 5 (spoils within days). */
public class Donation {
    public enum Status { AVAILABLE, SCHEDULED, PICKED_UP, DELIVERED, CANCELLED }

    private int id;
    private String donor, foodName, unit, location;
    private double quantity, remaining, distanceKm;
    private int perishability;
    private LocalDate harvestDate, bestBefore;
    private Status status = Status.AVAILABLE;

    public Donation(int id, String donor, String foodName, double quantity, String unit, String location,
                    double distanceKm, int perishability, LocalDate harvestDate, LocalDate bestBefore) {
        this.id = id; this.donor = donor; this.foodName = foodName; this.quantity = quantity;
        this.remaining = quantity; this.unit = unit; this.location = location; this.distanceKm = distanceKm;
        this.perishability = perishability; this.harvestDate = harvestDate; this.bestBefore = bestBefore;
    }

    public int getId() { return id; }
    public String getDonor() { return donor; }
    public String getFoodName() { return foodName; }
    public double getQuantity() { return quantity; }
    public double getRemaining() { return remaining; }
    public String getUnit() { return unit; }
    public String getLocation() { return location; }
    public double getDistanceKm() { return distanceKm; }
    public int getPerishability() { return perishability; }
    public LocalDate getHarvestDate() { return harvestDate; }
    public LocalDate getBestBefore() { return bestBefore; }
    public Status getStatus() { return status; }

    public void setStatus(Status s) { this.status = s; }
    public void setRemaining(double r) { this.remaining = r; }
    public void update(String donor, String foodName, double quantity, String unit, String location,
                       double distanceKm, int perishability, LocalDate harvestDate, LocalDate bestBefore) {
        double used = this.quantity - this.remaining;
        this.donor = donor; this.foodName = foodName; this.quantity = quantity; this.unit = unit;
        this.location = location; this.distanceKm = distanceKm; this.perishability = perishability;
        this.harvestDate = harvestDate; this.bestBefore = bestBefore;
        this.remaining = Math.max(0, quantity - used);
    }
}

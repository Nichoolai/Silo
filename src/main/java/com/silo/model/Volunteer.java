package com.silo.model;

public class Volunteer {
    private int id;
    private String name, phone, serviceArea, availability;
    private boolean active = true;

    public Volunteer(int id, String name, String phone, String serviceArea, String availability) {
        this.id = id; this.name = name; this.phone = phone; this.serviceArea = serviceArea; this.availability = availability;
    }
    public int getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getServiceArea() { return serviceArea; }
    public String getAvailability() { return availability; }
    public boolean isActive() { return active; }
    public void setActive(boolean a) { active = a; }
    public void update(String name, String phone, String serviceArea, String availability) {
        this.name = name; this.phone = phone; this.serviceArea = serviceArea; this.availability = availability;
    }
    @Override public String toString() { return name; }
}

package com.silo.model;

public class Recipient {
    private int id;
    private String name, contact, address, type;

    public Recipient(int id, String name, String contact, String address, String type) {
        this.id = id; this.name = name; this.contact = contact; this.address = address; this.type = type;
    }
    public int getId() { return id; }
    public String getName() { return name; }
    public String getContact() { return contact; }
    public String getAddress() { return address; }
    public String getType() { return type; }
    public void update(String name, String contact, String address, String type) {
        this.name = name; this.contact = contact; this.address = address; this.type = type;
    }
    @Override public String toString() { return name; }
}

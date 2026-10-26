package com.silo.algo;

import com.silo.model.Pickup;
import java.util.ArrayList;
import java.util.List;

/**
 * Pickup-processing queue. Pickups are enqueued as they are scheduled and processed in order of scheduled
 * time (earliest first; FIFO among equal times). Sorted-insertion list: enqueue O(n), peek O(1), dequeue O(n) shift.
 */
public class PickupQueue {
    private final List<Pickup> items = new ArrayList<>();

    public void enqueue(Pickup p) {
        int i = items.size();
        while (i > 0 && items.get(i - 1).getScheduledAt().isAfter(p.getScheduledAt())) i--;
        items.add(i, p);
    }
    public Pickup peek() { return items.isEmpty() ? null : items.get(0); }
    public Pickup dequeue() { return items.isEmpty() ? null : items.remove(0); }
    public boolean remove(Pickup p) { return items.remove(p); }
    public boolean isEmpty() { return items.isEmpty(); }
    public int size() { return items.size(); }
    public List<Pickup> toList() { return new ArrayList<>(items); }
}

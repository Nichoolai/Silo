package com.silo.algo;

import java.util.ArrayDeque;
import java.util.Deque;

/** Stack (LIFO) of undoable actions backing the "Undo last action" feature. */
public class ActionHistory {
    public interface Undoable { String describe(); void undo(); }

    private final Deque<Undoable> stack = new ArrayDeque<>();

    public void push(Undoable a) { stack.push(a); }
    public boolean isEmpty() { return stack.isEmpty(); }
    public String peekDescription() { return stack.isEmpty() ? null : stack.peek().describe(); }
    /** Undoes the most recent action; returns its description or null if the stack is empty. */
    public String undoLast() {
        if (stack.isEmpty()) return null;
        Undoable a = stack.pop();
        a.undo();
        return a.describe();
    }
}

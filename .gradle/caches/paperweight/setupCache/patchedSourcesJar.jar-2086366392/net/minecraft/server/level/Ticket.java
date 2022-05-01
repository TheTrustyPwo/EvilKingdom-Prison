package net.minecraft.server.level;

import java.util.Objects;

public final class Ticket<T> implements Comparable<Ticket<?>> {
    private final TicketType<T> type;
    private final int ticketLevel;
    public final T key;
    public long createdTick;
    public long delayUnloadBy; // Paper
    public int priority; // Paper - Chunk priority

    protected Ticket(TicketType<T> type, int level, T argument) {
        this.type = type;
        this.ticketLevel = level;
        this.key = argument;
        this.delayUnloadBy = type.timeout; // Paper
    }

    @Override
    public int compareTo(Ticket<?> ticket) {
        int i = Integer.compare(this.ticketLevel, ticket.ticketLevel);
        if (i != 0) {
            return i;
        } else {
            int j = Integer.compare(System.identityHashCode(this.type), System.identityHashCode(ticket.type));
            return j != 0 ? j : this.type.getComparator().compare(this.key, (T)ticket.key); // Paper - decompile fix
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof Ticket)) {
            return false;
        } else {
            Ticket<?> ticket = (Ticket)object;
            return this.ticketLevel == ticket.ticketLevel && Objects.equals(this.type, ticket.type) && Objects.equals(this.key, ticket.key);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.ticketLevel, this.key);
    }

    @Override
    public String toString() {
        return "Ticket[" + this.type + " " + this.ticketLevel + " (" + this.key + ")] at " + this.createdTick;
    }

    public TicketType<T> getType() {
        return this.type;
    }

    public int getTicketLevel() {
        return this.ticketLevel;
    }

    protected void setCreatedTick(long tickCreated) {
        this.createdTick = tickCreated;
    }

    protected boolean timedOut(long currentTick) {
        long l = delayUnloadBy; // Paper
        return l != 0L && currentTick - this.createdTick > l;
    }
}

package alternate.current.wire;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Iterator;

import net.minecraft.world.level.redstone.Redstone;

public class PowerQueue extends AbstractQueue<WireNode> {

    private static final int OFFSET = -Redstone.SIGNAL_MIN;

    /** The last wire for each power level. */
    private final WireNode[] tails;

    private WireNode head;
    private WireNode tail;

    private int size;

    public PowerQueue() {
        this.tails = new WireNode[(Redstone.SIGNAL_MAX + 1) - Redstone.SIGNAL_MIN];
    }

    @Override
    public boolean offer(WireNode wire) {
        if (wire == null) {
            throw new NullPointerException();
        }

        int power = wire.nextPower();

        if (contains(wire)) {
            if (wire.power == power) {
                // already queued for this power; exit
                return false;
            } else {
                // already queued for different power; move it
                move(wire, power);
            }
        } else {
            insert(wire, power);
        }

        return true;
    }

    @Override
    public WireNode poll() {
        if (head == null) {
            return null;
        }

        WireNode wire = head;
        WireNode next = wire.next;

        if (next == null) {
            clear(); // reset the tails array
        } else {
            if (wire.power != next.power) {
                // if the head is also a tail, its entry in the array
                // can be cleared; there is no previous wire with the
                // same power to take its place.
                tails[wire.power + OFFSET] = null;
            }

            wire.next = null;
            next.prev = null;
            head = next;

            size--;
        }

        return wire;
    }

    @Override
    public WireNode peek() {
        return head;
    }

    @Override
    public void clear() {
        for (WireNode wire = head; wire != null; ) {
            WireNode w = wire;
            wire = wire.next;

            w.prev = null;
            w.next = null;
        }

        head = null;
        tail = null;

        Arrays.fill(tails, null);

        size = 0;
    }

    @Override
    public Iterator<WireNode> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return size;
    }

    public boolean contains(WireNode wire) {
        return wire == head || wire.prev != null;
    }

    private void move(WireNode wire, int power) {
        remove(wire);
        insert(wire, power);
    }

    private void remove(WireNode wire) {
        if (wire == tail || wire.power != wire.next.power) {
            // assign a new tail for this wire's power
            if (wire == head || wire.power != wire.prev.power) {
                // there is no other wire with the same power; clear
                tails[wire.power + OFFSET] = null;
            } else {
                // the previous wire in the queue becomes the tail
                tails[wire.power + OFFSET] = wire.prev;
            }
        }

        if (wire == head) {
            head = wire.next;
        } else {
            wire.prev.next = wire.next;
        }
        if (wire == tail) {
            tail = wire.prev;
        } else {
            wire.next.prev = wire.prev;
        }

        wire.prev = null;
        wire.next = null;

        size--;
    }

    private void insert(WireNode wire, int power) {
        // store the power for which this wire is queued
        wire.power = power;

        // wires are sorted by power (highest to lowest)
        // wires with the same power are ordered FIFO
        if (head == null) {
            // first element in this queue \o/
            head = tail = wire;
        } else if (wire.power > head.power) {
            linkHead(wire);
        } else if (wire.power <= tail.power) {
            linkTail(wire);
        } else {
            // since the wire is neither the head nor the tail
            // findPrev is guaranteed to find a non-null element
            linkAfter(findPrev(wire), wire);
        }

        tails[power + OFFSET] = wire;

        size++;
    }

    private void linkHead(WireNode wire) {
        wire.next = head;
        head.prev = wire;
        head = wire;
    }

    private void linkTail(WireNode wire) {
        tail.next = wire;
        wire.prev = tail;
        tail = wire;
    }

    private void linkAfter(WireNode prev, WireNode wire) {
        linkBetween(prev, wire, prev.next);
    }

    private void linkBetween(WireNode prev, WireNode wire, WireNode next) {
        prev.next = wire;
        wire.prev = prev;

        wire.next = next;
        next.prev = wire;
    }

    private WireNode findPrev(WireNode wire) {
        WireNode prev = null;

        for (int i = wire.power + OFFSET; i < tails.length; i++) {
            prev = tails[i];

            if (prev != null) {
                break;
            }
        }

        return prev;
    }
}

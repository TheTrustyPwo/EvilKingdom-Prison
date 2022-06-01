package net.evilkingdom.prison.component.components.mine.objects;

/*
 * Made with love by https://kodirati.com/.
 */

public class MineLocation {

    private final int x, z;
    private boolean used;

    /**
     * Allows you to create a Mine Location.
     *
     * @param x ~ The mine location's x coordinate.
     * @param z ~ The mine location's z coordinate.
     * @param used ~ The mine location's usage state.
     */
    public MineLocation(final int x, final int z, final boolean used) {
        this.x = x;
        this.z = z;
        this.used = used;
    }

    /**
     * Allows you to retrieve the mine location's z coordinate.
     *
     * @return The mine location's z coordinate.
     */
    public Integer getZ() {
        return this.z;
    }

    /**
     * Allows you to retrieve the mine location's x coordinate.
     *
     * @return The mine location's x coordinate.
     */
    public Integer getX() {
        return this.x;
    }

    /**
     * Allows you to retrieve the mine location's usage state.
     *
     * @return The mine location's usage state.
     */
    public Boolean isUsed() {
        return this.used;
    }

    /**
     * Allows you to set the mine location's usage state.
     *
     * @param used ~ The mine location's usage state to set.
     */
    public void setUsed(final boolean used) {
        this.used = used;
    }

}

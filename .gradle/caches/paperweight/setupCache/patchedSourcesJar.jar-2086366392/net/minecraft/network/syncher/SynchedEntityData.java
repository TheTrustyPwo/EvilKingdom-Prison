package net.minecraft.network.syncher;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;

public class SynchedEntityData {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Object2IntMap<Class<? extends Entity>> ENTITY_ID_POOL = new Object2IntOpenHashMap();
    private static final int EOF_MARKER = 255;
    private static final int MAX_ID_VALUE = 254;
    private final Entity entity;
    private final Int2ObjectMap<SynchedEntityData.DataItem<?>> itemsById = new Int2ObjectOpenHashMap();
    // private final ReadWriteLock lock = new ReentrantReadWriteLock(); // Spigot - not required
    private boolean isEmpty = true;
    private boolean isDirty;

    public SynchedEntityData(Entity trackedEntity) {
        this.entity = trackedEntity;
    }

    public static <T> EntityDataAccessor<T> defineId(Class<? extends Entity> entityClass, EntityDataSerializer<T> dataHandler) {
        if (SynchedEntityData.LOGGER.isDebugEnabled()) {
            try {
                Class<?> oclass1 = Class.forName(Thread.currentThread().getStackTrace()[2].getClassName());

                if (!oclass1.equals(entityClass)) {
                    SynchedEntityData.LOGGER.debug("defineId called for: {} from {}", new Object[]{entityClass, oclass1, new RuntimeException()});
                }
            } catch (ClassNotFoundException classnotfoundexception) {
                ;
            }
        }

        int i;

        if (SynchedEntityData.ENTITY_ID_POOL.containsKey(entityClass)) {
            i = SynchedEntityData.ENTITY_ID_POOL.getInt(entityClass) + 1;
        } else {
            int j = 0;
            Class oclass2 = entityClass;

            while (oclass2 != Entity.class) {
                oclass2 = oclass2.getSuperclass();
                if (SynchedEntityData.ENTITY_ID_POOL.containsKey(oclass2)) {
                    j = SynchedEntityData.ENTITY_ID_POOL.getInt(oclass2) + 1;
                    break;
                }
            }

            i = j;
        }

        if (i > 254) {
            throw new IllegalArgumentException("Data value id is too big with " + i + "! (Max is 254)");
        } else {
            SynchedEntityData.ENTITY_ID_POOL.put(entityClass, i);
            return dataHandler.createAccessor(i);
        }
    }

    public boolean registrationLocked; // Spigot
    public <T> void define(EntityDataAccessor<T> key, T initialValue) {
        if (this.registrationLocked) throw new IllegalStateException("Registering datawatcher object after entity initialization"); // Spigot
        int i = key.getId();

        if (i > 254) {
            throw new IllegalArgumentException("Data value id is too big with " + i + "! (Max is 254)");
        } else if (this.itemsById.containsKey(i)) {
            throw new IllegalArgumentException("Duplicate id value for " + i + "!");
        } else if (EntityDataSerializers.getSerializedId(key.getSerializer()) < 0) {
            EntityDataSerializer datawatcherserializer = key.getSerializer();

            throw new IllegalArgumentException("Unregistered serializer " + datawatcherserializer + " for " + i + "!");
        } else {
            this.createDataItem(key, initialValue);
        }
    }

    private <T> void createDataItem(EntityDataAccessor<T> datawatcherobject, T t0) {
        SynchedEntityData.DataItem<T> datawatcher_item = new SynchedEntityData.DataItem<>(datawatcherobject, t0);

        // this.lock.writeLock().lock(); // Spigot - not required
        this.itemsById.put(datawatcherobject.getId(), datawatcher_item);
        this.isEmpty = false;
        // this.lock.writeLock().unlock(); // Spigot - not required
    }

    private <T> SynchedEntityData.DataItem<T> getItem(EntityDataAccessor<T> datawatcherobject) {
        // Spigot start
        /*
        this.lock.readLock().lock();

        DataWatcher.Item datawatcher_item;

        try {
            datawatcher_item = (DataWatcher.Item) this.itemsById.get(datawatcherobject.getId());
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Getting synched entity data");
            CrashReportSystemDetails crashreportsystemdetails = crashreport.addCategory("Synched entity data");

            crashreportsystemdetails.setDetail("Data ID", (Object) datawatcherobject);
            throw new ReportedException(crashreport);
        } finally {
            this.lock.readLock().unlock();
        }

        return datawatcher_item;
        */
        return (SynchedEntityData.DataItem) this.itemsById.get(datawatcherobject.getId());
        // Spigot end
    }

    public <T> T get(EntityDataAccessor<T> data) {
        return this.getItem(data).getValue();
    }

    public <T> void set(EntityDataAccessor<T> key, T value) {
        SynchedEntityData.DataItem<T> datawatcher_item = this.getItem(key);

        if (ObjectUtils.notEqual(value, datawatcher_item.getValue())) {
            datawatcher_item.setValue(value);
            this.entity.onSyncedDataUpdated(key);
            datawatcher_item.setDirty(true);
            this.isDirty = true;
        }

    }

    // CraftBukkit start - add method from above
    public <T> void markDirty(EntityDataAccessor<T> datawatcherobject) {
        this.getItem(datawatcherobject).setDirty(true);
        this.isDirty = true;
    }
    // CraftBukkit end

    public boolean isDirty() {
        return this.isDirty;
    }

    public static void pack(@Nullable List<SynchedEntityData.DataItem<?>> entries, FriendlyByteBuf buf) {
        if (entries != null) {
            Iterator iterator = entries.iterator();

            while (iterator.hasNext()) {
                SynchedEntityData.DataItem<?> datawatcher_item = (SynchedEntityData.DataItem) iterator.next();

                SynchedEntityData.writeDataItem(buf, datawatcher_item);
            }
        }

        buf.writeByte(255);
    }

    @Nullable
    public List<SynchedEntityData.DataItem<?>> packDirty() {
        List<SynchedEntityData.DataItem<?>> list = null;

        if (this.isDirty) {
            // this.lock.readLock().lock(); // Spigot - not required
            ObjectIterator objectiterator = this.itemsById.values().iterator();

            while (objectiterator.hasNext()) {
                SynchedEntityData.DataItem<?> datawatcher_item = (SynchedEntityData.DataItem) objectiterator.next();

                if (datawatcher_item.isDirty()) {
                    datawatcher_item.setDirty(false);
                    if (list == null) {
                        list = Lists.newArrayList();
                    }

                    list.add(datawatcher_item.copy());
                }
            }

            // this.lock.readLock().unlock(); // Spigot - not required
        }

        this.isDirty = false;
        return list;
    }

    @Nullable
    public List<SynchedEntityData.DataItem<?>> getAll() {
        List<SynchedEntityData.DataItem<?>> list = null;

        // this.lock.readLock().lock(); // Spigot - not required

        SynchedEntityData.DataItem datawatcher_item;

        for (ObjectIterator objectiterator = this.itemsById.values().iterator(); objectiterator.hasNext(); list.add(datawatcher_item.copy())) {
            datawatcher_item = (SynchedEntityData.DataItem) objectiterator.next();
            if (list == null) {
                list = Lists.newArrayList();
            }
        }

        // this.lock.readLock().unlock(); // Spigot - not required
        return list;
    }

    private static <T> void writeDataItem(FriendlyByteBuf buf, SynchedEntityData.DataItem<T> entry) {
        EntityDataAccessor<T> datawatcherobject = entry.getAccessor();
        int i = EntityDataSerializers.getSerializedId(datawatcherobject.getSerializer());

        if (i < 0) {
            throw new EncoderException("Unknown serializer type " + datawatcherobject.getSerializer());
        } else {
            buf.writeByte(datawatcherobject.getId());
            buf.writeVarInt(i);
            datawatcherobject.getSerializer().write(buf, entry.getValue());
        }
    }

    @Nullable
    public static List<SynchedEntityData.DataItem<?>> unpack(FriendlyByteBuf buf) {
        ArrayList arraylist = null;

        short short0;

        while ((short0 = buf.readUnsignedByte()) != 255) {
            if (arraylist == null) {
                arraylist = Lists.newArrayList();
            }

            int i = buf.readVarInt();
            EntityDataSerializer<?> datawatcherserializer = EntityDataSerializers.getSerializer(i);

            if (datawatcherserializer == null) {
                throw new DecoderException("Unknown serializer type " + i);
            }

            arraylist.add(SynchedEntityData.genericHelper(buf, short0, datawatcherserializer));
        }

        return arraylist;
    }

    private static <T> SynchedEntityData.DataItem<T> genericHelper(FriendlyByteBuf buf, int i, EntityDataSerializer<T> datawatcherserializer) {
        return new SynchedEntityData.DataItem<>(datawatcherserializer.createAccessor(i), datawatcherserializer.read(buf));
    }

    public void assignValues(List<SynchedEntityData.DataItem<?>> entries) {
        // this.lock.writeLock().lock(); // Spigot - not required

        try {
            Iterator iterator = entries.iterator();

            while (iterator.hasNext()) {
                SynchedEntityData.DataItem<?> datawatcher_item = (SynchedEntityData.DataItem) iterator.next();
                SynchedEntityData.DataItem<?> datawatcher_item1 = (SynchedEntityData.DataItem) this.itemsById.get(datawatcher_item.getAccessor().getId());

                if (datawatcher_item1 != null) {
                    this.assignValue(datawatcher_item1, datawatcher_item);
                    this.entity.onSyncedDataUpdated(datawatcher_item.getAccessor());
                }
            }
        } finally {
            // this.lock.writeLock().unlock(); // Spigot - not required
        }

        this.isDirty = true;
    }

    private <T> void assignValue(SynchedEntityData.DataItem<T> to, SynchedEntityData.DataItem<?> from) {
        if (!Objects.equals(from.accessor.getSerializer(), to.accessor.getSerializer())) {
            throw new IllegalStateException(String.format("Invalid entity data item type for field %d on entity %s: old=%s(%s), new=%s(%s)", to.accessor.getId(), this.entity, to.value, to.value.getClass(), from.value, from.value.getClass()));
        } else {
            to.setValue((T) from.getValue()); // CraftBukkit - decompile error
        }
    }

    public boolean isEmpty() {
        return this.isEmpty;
    }

    public void clearDirty() {
        this.isDirty = false;
        // this.lock.readLock().lock(); // Spigot - not required
        ObjectIterator objectiterator = this.itemsById.values().iterator();

        while (objectiterator.hasNext()) {
            SynchedEntityData.DataItem<?> datawatcher_item = (SynchedEntityData.DataItem) objectiterator.next();

            datawatcher_item.setDirty(false);
        }

        // this.lock.readLock().unlock(); // Spigot - not required
    }

    public static class DataItem<T> {

        final EntityDataAccessor<T> accessor;
        T value;
        private boolean dirty;

        public DataItem(EntityDataAccessor<T> data, T value) {
            this.accessor = data;
            this.value = value;
            this.dirty = true;
        }

        public EntityDataAccessor<T> getAccessor() {
            return this.accessor;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public T getValue() {
            return this.value;
        }

        public boolean isDirty() {
            return this.dirty;
        }

        public void setDirty(boolean dirty) {
            this.dirty = dirty;
        }

        public SynchedEntityData.DataItem<T> copy() {
            return new SynchedEntityData.DataItem<>(this.accessor, this.accessor.getSerializer().copy(this.value));
        }
    }
}

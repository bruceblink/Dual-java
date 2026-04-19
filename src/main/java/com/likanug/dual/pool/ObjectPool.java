package com.likanug.dual.pool;

import java.util.ArrayList;

import static processing.core.PApplet.println;

public class ObjectPool<T extends Poolable<T>> {
    private final int poolSize;
    private final ArrayList<T> pool;
    private int index = 0;
    private final ArrayList<T> temporalInstanceList;
    private int temporalInstanceCount = 0;
    private int allocationCount = 0;

    public ObjectPool(int pSize) {
        poolSize = pSize;
        pool = new ArrayList<T>(pSize);
        temporalInstanceList = new ArrayList<T>(pSize);
    }

    public ObjectPool() {
        this(256);
    }

    public int getPoolSize() {
        return poolSize;
    }

    public ArrayList<T> getPool() {
        return pool;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public ArrayList<T> getTemporalInstanceList() {
        return temporalInstanceList;
    }

    public int getTemporalInstanceCount() {
        return temporalInstanceCount;
    }

    public void setTemporalInstanceCount(int temporalInstanceCount) {
        this.temporalInstanceCount = temporalInstanceCount;
    }

    public int getAllocationCount() {
        return allocationCount;
    }

    public void setAllocationCount(int allocationCount) {
        this.allocationCount = allocationCount;
    }

    public T allocate() {
        if (!isAllocatable()) {
            throw new IllegalStateException("Object pool allocation failed: pool is exhausted (size=" + poolSize + ")");
        }
        T allocatedInstance = pool.get(index);

        allocatedInstance.setAllocated(true);
        allocatedInstance.setAllocationIdentifier(allocationCount);
        index++;
        allocationCount++;

        return allocatedInstance;
    }

    public T allocateTemporal() {
        T allocatedInstance = allocate();
        setTemporal(allocatedInstance);
        return allocatedInstance;
    }

    public void storeObject(T obj) {
        if (pool.size() >= poolSize) {
            throw new IllegalStateException("Failed to store a new instance to object pool: pool is already full (size=" + poolSize + ")");
        }
        pool.add(obj);
        obj.setBelongingPool(this);
        obj.setAllocationIdentifier(-1);
        obj.setAllocated(false);
    }

    public boolean isAllocatable() {
        return index < poolSize;
    }

    public void deallocate(T killedObject) {
        if (!killedObject.isAllocated()) {
            return;
        }

        killedObject.initialize();
        killedObject.setAllocated(false);
        killedObject.setAllocationIdentifier(-1);
        index--;
        pool.set(index, killedObject);
    }

    public void update() {
        while (temporalInstanceCount > 0) {
            temporalInstanceCount--;
            deallocate(temporalInstanceList.get(temporalInstanceCount));
        }
        temporalInstanceList.clear();    // not needed when array
    }

    public void setTemporal(T obj) {
        temporalInstanceList.add(obj);    // set when array
        temporalInstanceCount++;
    }
}

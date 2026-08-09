package com.likanug.dual.particle;

import com.likanug.dual.App;
import com.likanug.dual.pool.ObjectPool;

import java.util.ArrayList;

public class ParticleSet {

    private final ArrayList<Particle> particleList;
    private final ArrayList<Particle> removingParticleList;
    private final ObjectPool<Particle> particlePool;
    private final ParticleBuilder builder;


    public ParticleSet(int capacity, App app) {
        particlePool = new ObjectPool<>(capacity);
        for (int i = 0; i < capacity; i++) {
            particlePool.getPool().add(new Particle(app));
        }

        particleList = new ArrayList<>(capacity);
        removingParticleList = new ArrayList<>(capacity);
        builder = new ParticleBuilder(app);
    }

    public void update() {
        particlePool.update();

        for (Particle eachParticle : particleList) {
            eachParticle.update();
        }

        if (!removingParticleList.isEmpty()) {
            for (Particle eachInstance : removingParticleList) {
                particlePool.deallocate(eachInstance);
            }
            particleList.removeAll(removingParticleList);
            removingParticleList.clear();
        }
    }

    public void display() {
        for (Particle eachParticle : particleList) {
            eachParticle.display();
        }
    }

    public Particle allocate() {
        return particlePool.allocate();
    }

    public ArrayList<Particle> getParticleList() {
        return particleList;
    }

    public ArrayList<Particle> getRemovingParticleList() {
        return removingParticleList;
    }

    public ObjectPool<Particle> getParticlePool() {
        return particlePool;
    }

    public ParticleBuilder getBuilder() {
        return builder;
    }

    /** Returns active particles to the pool and clears transient effects between rounds. */
    public void clearForRound() {
        particlePool.update();
        for (Particle particle : removingParticleList) {
            particlePool.deallocate(particle);
        }
        for (Particle particle : particleList) {
            particlePool.deallocate(particle);
        }
        particleList.clear();
        removingParticleList.clear();
    }
}

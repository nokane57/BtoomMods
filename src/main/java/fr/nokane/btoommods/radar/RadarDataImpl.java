// fr/nokane/btoommods/radar/RadarDataImpl.java
package fr.nokane.btoommods.radar;

public class RadarDataImpl implements RadarData {
    private boolean implant = true;   // greffé par défaut
    private int boosters;
    private long lastMoveTick;
    private long cooldownUntil;

    @Override public boolean hasImplant() { return implant; }
    @Override public void setImplant(boolean v) { implant = v; }

    @Override public int getBoosters() { return boosters; }
    @Override public void setBoosters(int v) { boosters = v; }

    @Override public long getLastMoveTick() { return lastMoveTick; }
    @Override public void setLastMoveTick(long t) { lastMoveTick = t; }

    @Override public long getCooldownUntil() { return cooldownUntil; }
    @Override public void setCooldownUntil(long t) { cooldownUntil = t; }
}

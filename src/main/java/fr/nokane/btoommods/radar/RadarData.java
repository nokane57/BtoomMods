// fr/nokane/btoommods/radar/RadarData.java
package fr.nokane.btoommods.radar;

public interface RadarData {
    // Implant greffé (toujours présent par défaut)
    boolean hasImplant();
    void setImplant(boolean v);

    // Nombre de boosters (radars items en plus dans l'inventaire)
    int getBoosters();
    void setBoosters(int v);

    // Dernier tick où le joueur a bougé
    long getLastMoveTick();
    void setLastMoveTick(long t);

    long getCooldownUntil();
    void setCooldownUntil(long t);
}

package com.Graphic.models;

import com.Graphic.models.charms.CharmEffect;
import com.Graphic.models.charms.CharmId;

public class Charm {

    public final CharmId     id;
    public final String      name;
    public final String      imagePath;
    public final String      description;
    public final int         notchCost;
    public final CharmEffect effect;

    public boolean unlocked;
    public boolean equipped;

    public Charm(CharmId id, String name, String fileName,
                 String description, boolean unlocked,
                 int notchCost, CharmEffect effect) {
        this.id          = id;
        this.name        = name;
        this.imagePath   = "ui/inventory/charms/" + fileName;
        this.description = description;
        this.unlocked    = unlocked;
        this.notchCost   = notchCost;
        this.effect      = effect;
        this.equipped    = false;
    }
}

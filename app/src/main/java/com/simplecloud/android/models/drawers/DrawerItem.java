package com.simplecloud.android.models.drawers;

import java.io.Serializable;

public class DrawerItem implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private int id;
    private String name; // Display name
    private int iconId;
    private String title; // type of item (Accounts, Local ...)

    public DrawerItem(String title) {
        this(-100, null, 0);
        this.title = title;
    }

    public DrawerItem(int id, String name, int iconId) {
        this.id = id;
        this.name = name;
        this.iconId = iconId;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIcon() {
        return iconId;
    }

    public void setIcon(int icon) {
        this.iconId = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

}
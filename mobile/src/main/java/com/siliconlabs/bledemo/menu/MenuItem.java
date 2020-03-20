package com.siliconlabs.bledemo.menu;


import com.siliconlabs.bledemo.menu.MenuItemType;

public class MenuItem {
    private int menuIcon;
    private String title;
    private String description;
    private MenuItemType menuItemType;

    public MenuItem(int menuIcon, String title, String description, MenuItemType menuItemType) {
        this.menuIcon = menuIcon;
        this.title = title;
        this.description = description;
        this.menuItemType = menuItemType;
    }

    public int getMenuIcon() {
        return menuIcon;
    }

    public void setMenuIcon(int menuIcon) {
        this.menuIcon = menuIcon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MenuItemType getMenuItemType() {
        return menuItemType;
    }

    public void setMenuItemType(MenuItemType menuItemType) {
        this.menuItemType = menuItemType;
    }

    @Override
    public String toString() {
        return "MenuItem: " + title + " descr: " + description;
    }
}

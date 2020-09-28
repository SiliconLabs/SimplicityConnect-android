package com.siliconlabs.bledemo.Browser.Model;


public class SavedSearch {
    private String searchText;

    public SavedSearch(String searchText) {
        this.searchText = searchText;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }
}

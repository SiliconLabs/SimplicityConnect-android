package com.siliconlabs.bledemo.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.siliconlabs.bledemo.mappings.Mapping;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashSet;

import static android.content.Context.MODE_PRIVATE;

public class SharedPrefUtils {
    private SharedPreferences mPrefs;
    private SharedPreferences.Editor editor;
    private Gson gson;
    private static final String MAP_KEY = "MAP_KEY";
    private static final String LAST_FILTER_KEY = "LAST_FILTER_KEY";
    private static final String FAVORITES_DEVICES_KEY = "FAVORITES_DEVICES_KEY";
    private static final String TEMPORARY_FAVORITES_DEVICES_KEY = "TEMPORARY_FAVORITES_DEVICES_KEY";
    private static final String DISPLAY_BROWSER_LEAVE_DIALOG_KEY = "DISPLAY_BROWSER_LEAVE_DIALOG_KEY";
//    private static final String LOGS_KEY = "LOGS_KEY";

    private static final String CHARACTERISTIC_NAMES_KEY = "CHARACTERISTIC_NAMES_KEY";
    private static final String SERVICE_NAMES_KEY = "SERVICE_NAMES_KEY";

    public SharedPrefUtils(Context context) {
        mPrefs = context.getSharedPreferences("MODEL_PREFERENCES", MODE_PRIVATE);
        editor = mPrefs.edit();
        gson = new Gson();
    }

    public HashMap<String, FilterDeviceParams> getMapFilter() {
        if (getString(MAP_KEY) == null) {
            return new HashMap<>();
        } else {
            Type type = new TypeToken<HashMap<String, FilterDeviceParams>>() {
            }.getType();
            return gson.fromJson(getString(MAP_KEY), type);
        }
    }

//    public List<Log> getLogs(){
//        if (getString(LOGS_KEY) == null) {
//            return new ArrayList<>();
//        } else {
//            Type type = new TypeToken<List<Log>>() {
//            }.getType();
//            return gson.fromJson(getString(LOGS_KEY), type);
//        }
//    }
//
//    public void addLog(Log log){
//        List<Log> logs = getLogs();
//        logs.add(log);
//        String json = gson.toJson(logs);
//        editor.putString(LOGS_KEY, json);
//        editor.commit();
//    }
//
//    public void removeLogs(){
//        editor.remove(LOGS_KEY);
//        editor.commit();
//    }

    public LinkedHashSet<String> getFavoritesDevices() {
        if (getString(FAVORITES_DEVICES_KEY) == null) {
            return new LinkedHashSet<>();
        } else {
            Type type = new TypeToken<LinkedHashSet<String>>() {
            }.getType();
            return gson.fromJson(getString(FAVORITES_DEVICES_KEY), type);
        }
    }

    public LinkedHashSet<String> getTemporaryFavoritesDevices() {
        if (getString(TEMPORARY_FAVORITES_DEVICES_KEY) == null) {
            return new LinkedHashSet<>();
        } else {
            Type type = new TypeToken<LinkedHashSet<String>>() {
            }.getType();
            return gson.fromJson(getString(TEMPORARY_FAVORITES_DEVICES_KEY), type);
        }
    }

    public void mergeTmpDevicesToFavorites() {
        LinkedHashSet<String> favoritesDevices = getFavoritesDevices();
        LinkedHashSet<String> temporaryFavoritesDevices = getTemporaryFavoritesDevices();
        favoritesDevices.addAll(temporaryFavoritesDevices);
        String json = gson.toJson(favoritesDevices);
        editor.putString(FAVORITES_DEVICES_KEY, json);
        editor.commit();
    }

    public void addDeviceToFavorites(String device) {
        LinkedHashSet<String> favoritesDevices = getFavoritesDevices();
        favoritesDevices.add(device);
        String json = gson.toJson(favoritesDevices);
        editor.putString(FAVORITES_DEVICES_KEY, json);
        editor.commit();
    }

    public void addDeviceToTemporaryFavorites(String device) {
        LinkedHashSet<String> temporaryFavoritesDevices = getTemporaryFavoritesDevices();
        temporaryFavoritesDevices.add(device);
        String json = gson.toJson(temporaryFavoritesDevices);
        editor.putString(TEMPORARY_FAVORITES_DEVICES_KEY, json);
        editor.commit();
    }

    public void removeDeviceFromFavorites(String device) {
        LinkedHashSet<String> favoritesDevices = getFavoritesDevices();
        favoritesDevices.remove(device);
        String json = gson.toJson(favoritesDevices);
        editor.putString(FAVORITES_DEVICES_KEY, json);
        editor.commit();
    }

    public void removeDeviceFromTemporaryFavorites(String device) {
        LinkedHashSet<String> temporaryFavoritesDevices = getTemporaryFavoritesDevices();
        temporaryFavoritesDevices.remove(device);
        String json = gson.toJson(temporaryFavoritesDevices);
        editor.putString(TEMPORARY_FAVORITES_DEVICES_KEY, json);
        editor.commit();
    }

    public boolean isFavorite(String device) {
        if (getString(FAVORITES_DEVICES_KEY) == null) return false;
        return getFavoritesDevices().contains(device);
    }

    public boolean isTemporaryFavorite(String device) {
        if (getString(TEMPORARY_FAVORITES_DEVICES_KEY) == null) return false;
        return getTemporaryFavoritesDevices().contains(device);
    }

    private String getString(String key) {
        return mPrefs.getString(key, null);
    }

    public boolean isExistFilterWithKey(String key) {
        if (getString(MAP_KEY) == null) return false;
        return getMapFilter().containsKey(key);
    }

    public void addToMapFilterAndSave(String key, FilterDeviceParams filterDeviceParam) {
        HashMap<String, FilterDeviceParams> mapFilter = getMapFilter();
        mapFilter.put(key, filterDeviceParam);
        String json = gson.toJson(mapFilter);
        editor.putString(MAP_KEY, json);
        editor.commit();
    }

    public void updateMapFilter(HashMap<String, FilterDeviceParams> currentMap) {
        String json = gson.toJson(currentMap);
        editor.putString(MAP_KEY, json);
        editor.commit();
    }

    public FilterDeviceParams getLastFilter() {
        if (getString(LAST_FILTER_KEY) != null) {
            FilterDeviceParams filterDeviceParams = gson.fromJson(getString(LAST_FILTER_KEY), FilterDeviceParams.class);
            return filterDeviceParams.isEmptyFilter() ? null : filterDeviceParams;
        }
        return null;
    }

    public void setLastFilter(FilterDeviceParams lastFilter) {
        String json = gson.toJson(lastFilter);
        editor.putString(LAST_FILTER_KEY, json);
        editor.commit();
    }

    // =============== SAVING characteristic/service uuid -> custom name mapings

    public HashMap<String, Mapping> getCharacteristicNamesMap() {
        String defValue = new Gson().toJson(new HashMap<String, String>());
        String json = mPrefs.getString(CHARACTERISTIC_NAMES_KEY, defValue);
        TypeToken<HashMap<String, Mapping>> token = new TypeToken<HashMap<String, Mapping>>() {
        };

        return gson.fromJson(json, token.getType());
    }

    public HashMap<String, Mapping> getServiceNamesMap() {
        String defValue = new Gson().toJson(new HashMap<String, String>());
        String json = mPrefs.getString(SERVICE_NAMES_KEY, defValue);
        TypeToken<HashMap<String, Mapping>> token = new TypeToken<HashMap<String, Mapping>>() {
        };

        return gson.fromJson(json, token.getType());
    }

    public void saveCharacteristicNamesMap(HashMap<String, Mapping> map) {
        String json = gson.toJson(map);
        editor.putString(CHARACTERISTIC_NAMES_KEY, json);
        editor.apply();
    }

    public void saveServiceNamesMap(HashMap<String, Mapping> map) {
        String json = gson.toJson(map);
        editor.putString(SERVICE_NAMES_KEY, json);
        editor.apply();
    }

    public boolean shouldDisplayLeaveBrowserDialog() {
        boolean displayDialog = mPrefs.getBoolean(DISPLAY_BROWSER_LEAVE_DIALOG_KEY,true);
        return displayDialog;
    }

    public void setShouldDisplayLeaveBrowserDialog(boolean displayDialog) {
        editor.putBoolean(DISPLAY_BROWSER_LEAVE_DIALOG_KEY,displayDialog);
        editor.apply();
    }

}

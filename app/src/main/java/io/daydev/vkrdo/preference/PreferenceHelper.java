package io.daydev.vkrdo.preference;

import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.daydev.vkrdo.bean.RadioInfo;

import java.util.*;

/**
 * Created by dmitry on 05.03.15.
 */
public class PreferenceHelper {

    public static <V> void saveCollection(SharedPreferences sharedPreferences, String key, Collection<V> list){
        Gson gson = new GsonBuilder().create();
        String hashMapString = gson.toJson(list);

        sharedPreferences
                .edit()
                .putString(key, hashMapString)
                .apply();
    }

    public static <K,V> void  saveMap(SharedPreferences sharedPreferences, String key, Map<K,V> obj){
        Gson gson = new GsonBuilder().create();
        String hashMapString = gson.toJson(obj);

        sharedPreferences
                .edit()
                .putString(key, hashMapString)
                .apply();
    }

    public static void clean(SharedPreferences sharedPreferences, String key){
        sharedPreferences
                .edit()
                .remove(key)
                .apply();
    }

    public static  Map<String, RadioInfo> getMap(SharedPreferences sharedPreferences, String key){
        String value = sharedPreferences.getString(key, null);
        if (value == null || value.isEmpty()){
            return null;
        }

        Gson gson = new GsonBuilder().create();
        return gson.fromJson(value, new TypeToken<HashMap<String, RadioInfo>>(){}.getType());
    }

    public static ArrayList<String> getCollection(SharedPreferences sharedPreferences, String key){
        String value = sharedPreferences.getString(key, null);
        if (value == null || value.isEmpty()){
            return null;
        }

        Gson gson = new GsonBuilder().create();
        return gson.fromJson(value, new TypeToken<ArrayList<String>>(){}.getType());
    }



}

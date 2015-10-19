package com.example.keinsfield.vacapp.Mundo;

import android.app.Activity;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Santiago on 19/10/2015.
 */
public class CowPersistenceManager {

    public static String published_at;
    private static HashMap<CowKey,Cow> cowList;

    public static HashMap<CowKey,Cow> getCows(Activity context){
        if(cowList == null) cowList = getCowsFromDBFile(context);
        return cowList;
    }

    private static HashMap<CowKey,Cow> getCowsFromDBFile(Activity context){
        try {
            File file = Utilities.getCowBD(context);
            if (file == null) return (new HashMap<>());
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            HashMap<CowKey,Cow> otList = (HashMap<CowKey,Cow>)ois.readObject();
            ois.close();
            return (otList);
        } catch (Exception e) {
            Log.d("SC", "Problems reading from file. " + e.getMessage());
            return (new HashMap<>());
        }
    }

    /* Merge con prevalencia en map1. Modifica map1.*/
    private static HashMap<CowKey,Cow> merge(HashMap<CowKey,Cow> map1, HashMap<CowKey,Cow> map2){
        for(CowKey key:map2.keySet()){
            if(!map1.containsKey(key))map1.put(key,map2.get(key));
        }
        return map1;
    }
    public static void addCow(Cow cow,Activity context) throws Exception{
        cowList = getCows(context);
        if(!cowList.containsKey(new CowKey(cow))) cowList.put(new CowKey(cow),cow);
        updateDBFile(context);
    }

    /*Fills the arraylist with info from images and file. Updates cowDB file. */
    public static void syncWithLocal(Activity context){
        try {
            //First get all cows from file.
            cowList = getCows(context);
            cowList = merge(cowList,getCowsFromDBFile(context));
            //Find cows in VACAPP images.
            HashMap<String, ArrayList<File>> map = Utilities.getAllVacappImages(context);
            for (String farm : map.keySet()) {
                ArrayList<File> cows = map.get(farm);
                for (File cowFile : cows) {
                    int nv = Utilities.getCowNumberFromFile(cowFile);
                    CowKey key = new CowKey(nv, farm);
                    if (!cowList.containsKey(key)) {
                        cowList.put(key, new Cow(key));
                    }
                }
            }
            //Update file with merged info.
            updateDBFile(context);
        }
        catch(Exception e){
            Log.d("SC","Problems with localsync: "+e.getMessage());
        }

    }

    /*Replaces the cowDB file with a new one containing the cows in cowList
    * In most cases, this shouldn't be used, instead use sync methods to avoid
    * losing information*/
    private static void updateDBFile(Activity context) throws Exception{
        File file = Utilities.getCowBD(context);
        if(file != null) file.delete();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(cowList);
        oos.close();
    }

    public static void syncWithWebService(){

    }
}

class CowKey{
    public String farm;
    public int nv;

    public CowKey(int n, String farm){
        nv = n;
        this.farm = farm;
    }

    public CowKey(Cow cow){
        nv = cow.nv;
        this.farm = cow.finca;
    }

    public boolean equals (Object o){
        CowKey oKey = (CowKey)o;
        return nv == oKey.nv && farm.equals(oKey.farm);
    }
}

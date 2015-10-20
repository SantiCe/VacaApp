package com.example.keinsfield.vacapp.Mundo;

import java.io.Serializable;

/**
 * Created by Santiago on 19/10/2015.
 */
public class CowKey implements Serializable{
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
        return (nv == oKey.nv) && (farm.equals(oKey.farm));
    }

    public int hashCode(){
        return nv+farm.hashCode();
    }
}

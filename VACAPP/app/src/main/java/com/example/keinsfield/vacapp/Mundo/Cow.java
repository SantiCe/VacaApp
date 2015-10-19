package com.example.keinsfield.vacapp.Mundo;

import java.util.List;

/**
 * Created by Santiago on 18/10/2015.
 */
public class Cow
{
    public String nombre;
    public String finca;
    public int nv;
    public String ultimo_parto;
    public int hato;
    public String loc;
    public int partos;
    public int dias_lac;
    public int lts_dia;
    public String primer_servicio;

    public boolean equals(Object ot){
        Cow oCow = (Cow)ot;
        return (finca.equals(oCow.finca) && nv == oCow.nv);
    }

    public Cow(CowKey key){
        this.nv = key.nv;
        this.finca = key.farm;
    }
}


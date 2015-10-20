package com.example.keinsfield.vacapp.Mundo;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Santiago on 18/10/2015.
 */
public class Cow implements Serializable
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
        nombre = "";
        ultimo_parto = "";
        hato = -1;
        loc = "";
        partos = -1;
        dias_lac = -1;
        lts_dia= -1;
        primer_servicio = "";
    }

    public Cow(String nombre, String finca, int nv, String ultimo_parto, int hato, String loc, int partos, int dias_lac, int lts_dia, String primer_servicio){
        this.nombre = nombre;
        this.finca = finca;
        this.nv = nv;
        this.ultimo_parto = ultimo_parto;
        this.hato = hato;
        this.loc =loc;
        this.partos = partos;
        this.dias_lac = dias_lac;
        this.lts_dia = lts_dia;
        this.primer_servicio = primer_servicio;
    }

    public String toString(){
        String bs = "\n";
        String s = "Nombre: "+nombre+"      Finca: "+finca+bs;
        s+="Id: "+nv+"      Hato: "+hato;
        return s;
    }
}


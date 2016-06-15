package com.euromonitor;

import java.util.ArrayList;
import java.util.List;

import java.util.Locale;
/**
 * Created by Shashwat.Bajpai on 6/15/2016.
 */
public class Country
{

    //private List<String> listOfCountries;

    public ArrayList<String> getAllCountryName()
    {

        ArrayList<String> temp=new ArrayList<>();
        String result="";
        String[] country=Locale.getISOCountries();
        for(String countryCode:country)
        {
            Locale obj = new Locale("", countryCode);
             result=obj.getDisplayCountry();
            temp.add(result);
        }
        return temp ;
 }

}

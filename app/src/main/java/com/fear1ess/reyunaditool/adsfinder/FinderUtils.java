package com.fear1ess.reyunaditool.adsfinder;

import com.fear1ess.reyunaditool.IDoCommandService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FinderUtils {
    public static Map<String,String> adsInfoMap = new HashMap<>();
    public static List<Finder> finderArray = new ArrayList<>();

    static{
        adsInfoMap.put("admob", "com.google.android.gms.ads.MobileAds");
        adsInfoMap.put("unity", "com.unity3d.ads.UnityAds");
    }

    public static Finder createFinder(String adsName, String adsClsName, ClassLoader cl, IDoCommandService service){
        switch(adsName){
            case "admob":
                return new AdmobFinder(adsName, adsClsName, cl, service);
            case "unity":
                return new UnityFinder(adsName, adsClsName, cl, service);
            default:
                return new DefaultFinder(adsName, adsClsName, cl, service);
        }
    }

    public static void notifyServiceBound(IDoCommandService service){
        for(Finder finder : finderArray){
            finder.uploadRemainAdsData(service);
        }
    }

    public static void doWork(ClassLoader cl, IDoCommandService service){
        for(Map.Entry<String,String> entry : adsInfoMap.entrySet()){
            Finder finder = createFinder(entry.getKey(), entry.getValue(), cl, service);
            finderArray.add(finder);
            finder.startWork();
        }
    }
}

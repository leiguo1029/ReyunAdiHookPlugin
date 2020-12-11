package com.fear1ess.reyunaditool.adsfinder;

import com.fear1ess.reyunaditool.IDoCommandService;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class UnityFinder extends Finder {
    public UnityFinder(String adsName, String adsClassName, ClassLoader cl, IDoCommandService service) {
        super(adsName, adsClassName, cl, service);
    }

    @Override
    public void hookAdsApi() {
        try {
            XposedBridge.hookAllMethods(mAppClassLoader.loadClass("com.unity3d.ads.UnityAds"), "initialize",
                    new AdsApiHook("AD", "unityGameId", 1));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}

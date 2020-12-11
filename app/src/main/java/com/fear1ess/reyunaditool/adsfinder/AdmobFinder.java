package com.fear1ess.reyunaditool.adsfinder;

import android.content.Context;
import android.util.Log;

import com.fear1ess.reyunaditool.HookEntry;
import com.fear1ess.reyunaditool.IDoCommandService;
import com.fear1ess.reyunaditool.OperateCmd;

import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class AdmobFinder extends Finder {
    public AdmobFinder(String adsClassName, String adsName, ClassLoader cl, IDoCommandService service) {
        super(adsClassName, adsName, cl, service);
    }

    public class AdmobApiHook extends AdsApiHook{

        private AdmobApiHook(String s, String dataName, int posInArgs){
            super(s, dataName, posInArgs);
        }

        public AdmobApiHook(String s, int posInArgs){
            this(s, "data", posInArgs);
        }

        public Map buildAdsData(String adUnitId){
            String[] vals = adUnitId.split("/");
            if(vals.length != 2) return null;
            Map<String,String> map = new HashMap<>();
            map.put("client", vals[0]);
            map.put("slotname", vals[1]);
            return map;
        }
    }

    public void hookAdsApi() {
        XposedHelpers.findAndHookMethod("com.google.android.gms.ads.BaseAdView", mAppClassLoader, "setAdUnitId",
                String.class, new AdmobApiHook("bannerAd", 0));

        XposedHelpers.findAndHookConstructor("com.google.android.gms.ads.AdLoader$Builder", mAppClassLoader,
                Context.class, String.class, new AdmobApiHook("nativeAd", 1));

        XposedHelpers.findAndHookConstructor("com.google.android.gms.ads.rewarded.RewardedAd", mAppClassLoader,
                Context.class, String.class, new AdmobApiHook("rewardedAd", 1));

        try {
            /*
            XposedBridge.hookAllMethods(mAppClassLoader.loadClass("com.google.android.gms.ads.reward.RewardedVideoAd"), "loadAd",
                    new AdmobApiHook("rewardedAd(legacy)", 0));*/

            XposedBridge.hookAllMethods(mAppClassLoader.loadClass("import com.google.android.gms.ads.appopen.AppOpenAd"), "load",
                    new AdmobApiHook("openAd", 1));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}

package com.fear1ess.reyunaditool.adsfinder;

import com.fear1ess.reyunaditool.IDoCommandService;

public class DefaultFinder extends Finder {
    public DefaultFinder(String adsName, String adsClassName, ClassLoader cl, IDoCommandService service) {
        super(adsName, adsClassName, cl, service);
    }

    @Override
    public void hookAdsApi() {
        return;
    }
}

package com.fear1ess.reyunaditool.adsfinder;

import android.util.Log;

import com.fear1ess.reyunaditool.HttpParser;
import com.fear1ess.reyunaditool.IDoCommandService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FacebookFinder extends Finder {

    public FacebookFinder(String adsName, String adsClassName, ClassLoader cl, IDoCommandService service) {
        super(adsName, adsClassName, cl, service);
    }

    @Override
    public void hookAdsApi() {
        registerSSLHook(new SSLOutputStreamHookedCallback() {
            @Override
            public void onSSLOutputStreamHooked(HttpParser hp) {
                if(!hp.getMethod().equals("POST")) return;
                if(!hp.getPath().contains("network_ads_common") && (!hp.getPath().contains("/adnw_sync"))) return;
                Map<String, String> map = new HashMap<>();
                // PLACEMENT_ID, IDFA, M_BANNER_KEY, PLACEMENT_TYPE, APPNAME, AFP, ASHAS
                if(hp.getPath().contains("network_ads_common")){
                    String placementId = hp.getFormBodyParam("PLACEMENT_ID");
                    String idfa = hp.getFormBodyParam("IDFA");
                    String afp = hp.getFormBodyParam("AFP");
                    String ashas = hp.getFormBodyParam("ASHAS");
                    String mBannerKey = hp.getFormBodyParam("M_BANNER_KEY");
                    String placementType = hp.getFormBodyParam("PLACEMENT_TYPE");
                    map.put("PLACEMENT_ID", placementId);
                    map.put("IDFA", idfa);
                    map.put("AFP", afp);
                    map.put("ASHAS", ashas);
                    map.put("PLACEMENT_TYPE", placementType);
                    map.put("M_BANNER_KEY", mBannerKey);
                }else{
                    String payload = (String) hp.getFormBodyParam("payload");
                    try {
                        JSONObject jo = new JSONObject(payload);
                        JSONObject context = jo.getJSONObject("context");
                        String placementId = context.getString("PLACEMENT_ID");
                        String idfa = context.getString("IDFA");
                        String afp = context.getString("AFP");
                        String ashas = context.getString("ASHAS");
                        map.put("PLACEMENT_ID", placementId);
                        map.put("IDFA", idfa);
                        map.put("AFP", afp);
                        map.put("ASHAS", ashas);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                uploadAdsData(map);
            }
        });
    }
}

// by fear1ess 2020/12/25

package com.fear1ess.reyunaditool;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class HttpParser {
    private String method;
    private String path;
    private Map<String, String> urlParams = new HashMap<>();
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> formBodyParams = null;
    private Map<String, Object> jsonBodyParams = null;
    private JSONObject jsonBody = null;

    public String getMethod(){
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getUrlParam(String key){
        return urlParams.get(key);
    }

    public String getHeader(String key) {
        return headers.get(key);
    }

    public String getFormBodyParam(String key) {
        return formBodyParams.get(key);
    }

    public Object getjsonObjectParam(String key) {
        return jsonBodyParams.get(key);
    }

    public JSONObject getJsonBody(){
        return jsonBody;
    }

    public HttpParser(byte[] payload){
        doParse(new String(payload));
    }

    public HttpParser(String payload){
        doParse(payload);
    }

    private boolean doParse(String payload) {
        String httpStr = new String(payload);
        if (!httpStr.startsWith("GET") && !httpStr.startsWith("POST")) return false;
        try {
            String[] items = httpStr.split("\r\n\r\n");
            String urlAndHeaderItem = items[0];
            String[] items_1 = urlAndHeaderItem.split("\r\n", 2);
            String urls = items_1[0];
            String[] items_2 = urls.split(" ");
            method = items_2[0];
            String[] items_3 = items_2[1].split("\\?");
            path = items_3[0];
            if (items_3.length > 1) {
                String[] urlParamStrs = items_3[1].split("&");
                for (String item : urlParamStrs) {
                    String[] paramItem = item.split("=");
                    String key = URLDecoder.decode(paramItem[0], "utf-8");
                    String value = URLDecoder.decode(paramItem[1], "utf-8");
                    urlParams.put(key, value);
                }
            }
            if(items_1.length > 1){
                String headerStr = items_1[1];
                String[] headerStrs = headerStr.split("\r\n");
                headers = new HashMap<>();
                for (String item : headerStrs) {
                    String[] headerItem = item.split(": ");
                    String key = URLDecoder.decode(headerItem[0], "utf-8");
                    String value = URLDecoder.decode(headerItem[1], "utf-8");
                    headers.put(key, value);
                }
            }

            if (items.length > 1 && httpStr.startsWith("POST")) {
                String body = items[1];
                if(headers.get("Content-Type").contains("application/x-www-form-urlencoded")){
                    formBodyParams = new HashMap<>();
                    String[] urlParamStrs = body.split("&");
                    for (String item : urlParamStrs) {
                        String[] paramItem = item.split("=");
                        String key = URLDecoder.decode(paramItem[0], "utf-8");
                        String value = URLDecoder.decode(paramItem[1], "utf-8");
                        formBodyParams.put(key, value);
                    }
                }

                if(headers.get("Content-Type").contains("application/json")){
                    jsonBodyParams = new HashMap<>();
                    JSONObject jo = new JSONObject(body);
                    jsonBody = jo;
                    Iterator iterator = jo.keys();
                    while(iterator.hasNext()){
                        String key = (String) iterator.next();
                        jsonBodyParams.put(key, jo.get(key));
                    }
                }
            }

        } catch (UnsupportedEncodingException | JSONException e) {
            e.printStackTrace();
        }
        return true;
    }
}

package com.hunterltd.ssw.curse;

import org.json.simple.JSONObject;

@SuppressWarnings("unchecked")
public class CurseAddon extends JSONObject {
    public CurseAddon(JSONObject obj) {
        this.putAll(obj);
    }

    
}

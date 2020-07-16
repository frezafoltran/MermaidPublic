package com.foltran.mermaid.model.trip;

import java.util.Map;

public class ShortTextCard extends TripCard {

    String txt = "";
    String previewTxt = "";
    int maxCharToPreview = 20;

    public ShortTextCard(Map<String, Object> params){

        if (params != null){
            txt = (String) params.get("text");
        }
        this.setType("shortTextCard");
        this.needsPager = false;
    }

    public void setText(String txt) {
        this.txt = txt;

        if (txt.length() > maxCharToPreview) {
            this.previewTxt = txt.substring(0, maxCharToPreview) + "...";
        }
        else{
            this.previewTxt = txt;
        }
    }

    public String getText() {return this.txt; }

    public String getPreviewTxt(){
        return this.previewTxt;
    }

}

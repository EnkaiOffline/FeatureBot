package org.enkai.featurebot.features.openai.objects;

public class Usage {

    public int prompt_tokens;
    public int completion_tokens;
    public int total_tokens;

    public Usage() {

    }

    public Usage(int total_tokens) {
        this.total_tokens = total_tokens;
    }


}

package org.enkai.featurebot.features.openai.objects;

public class Choice {

    public ChatMessage message;
    public String finish_reason;
    public int index;

    public Choice() {

    }

    public Choice(String answer) {
        message = new ChatMessage("assistant", answer);
    }

}

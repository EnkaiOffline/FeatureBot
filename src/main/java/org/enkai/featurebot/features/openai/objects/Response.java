package org.enkai.featurebot.features.openai.objects;

public class Response {

    public String model;
    public String id;
    public String object;
    public long created;
    public Usage usage;
    public Choice[] choices;

}

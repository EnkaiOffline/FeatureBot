package org.enkai.featurebot.features.openai.objects;


public class Request {

    public String model = "gpt-3.5-turbo-0301";
    public ChatMessage[] messages;
    public double temperature;
    public int max_tokens = 2024;
    public int n = 1;

}

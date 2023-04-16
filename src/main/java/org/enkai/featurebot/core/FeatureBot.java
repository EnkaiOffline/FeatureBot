package org.enkai.featurebot.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class FeatureBot extends TelegramLongPollingBot {

    private List<Feature> features;
    private Map<Pattern, Method> methods = new HashMap<>();
    protected Logger log = LoggerFactory.getLogger(this.getClass());

    public FeatureBot(String token) {
        super(token);
        features = new ArrayList<>();
    }

    public void addFeature(Feature feature) {
        features.add(feature);
        scanFeature(feature);
    }

    private void scanFeature(Feature feature) {
        Class<?> featureClass = feature.getClass();
        for(Method method : featureClass.getMethods()) {
            for(Annotation annotation : method.getDeclaredAnnotations()) {
                if(annotation instanceof Command) {
                    Pattern pattern = Pattern.compile(((Command) annotation).value());
                    methods.put(pattern, method);
                    log.info("{} feature: regex \"{}\" associated with {}() method", feature.getName(), pattern, method.getName());
                }
            }
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasCallbackQuery()) {
            processCallbackQuery(update.getCallbackQuery());
        }
        if(update.hasMessage() && update.getMessage().hasText()) {
            String input = update.getMessage().getText();
            if(input.startsWith("@" + getBotUsername())) {//Remove preceding @BotName
                input = input.substring(getBotUsername().length() + 1);
            }
            User user = update.getMessage().getFrom();
            log.info("Username: {}; Tag: {} {}; Id: {}; Input: {};", user.getFirstName(), (user.getLastName() == null ? "" : user.getLastName()), user.getUserName(), user.getId(), input);
            for(Map.Entry<Pattern, Method> entry : methods.entrySet()) {
                //Finding base object
                Class<?> originClass = entry.getValue().getDeclaringClass();
                Feature origin = null;
                for (Feature feature: features) {
                    if(feature.getClass().equals(originClass)) {
                        origin = feature;
                        break;
                    }
                }
                //Invoking
                if(entry.getKey().matcher(input).matches()) {
                    try {
                        invokeInNewThread(origin, entry.getValue(), update.getMessage());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private void processCallbackQuery(CallbackQuery query) {

    }

    private void invokeInNewThread(Object origin, Method method, Message message) {
        log.info("Invoking method:\"{}\", for input:\"{}\"", method.getName(), message.getText());
        new Thread(() -> {
            try {
                method.invoke(origin, message);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }).start();
    }

    //Useful methods

    public boolean sendText(Message message, String text, boolean isReply) {
        log.info("Sending message to \"{}\", text: \"{}\", is reply: {}", message.getFrom().getUserName(), text, isReply);
        SendMessage.SendMessageBuilder sendMessage = SendMessage.builder()
                .chatId(message.getChatId() + "")
                .text(text);
        if(isReply) {
            sendMessage.replyToMessageId(message.getMessageId());
        }
        try {
            log.info("Message sent successfully");
            execute(sendMessage.build());
            return true;
        } catch (TelegramApiException e) {
            log.info("Error sending message");
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String getBotUsername() {
        return System.getenv("TelegramBotUsername");
    }

}

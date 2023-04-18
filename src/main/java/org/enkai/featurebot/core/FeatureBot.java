package org.enkai.featurebot.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
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
            if(input.startsWith("/") && input.endsWith("@" + getBotUsername())) {//Remove ending @BotName
                input = input.substring(0, input.length() - getBotUsername().length() - 1);
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
        Feature targetFeature = null;
        for(Feature feature : features) {
            if(query.getData().startsWith(feature.getName())) {
                targetFeature = feature;
                break;
            }
        }
        if(targetFeature != null) {
            query.setData(query.getData().substring(targetFeature.getName().length()));
            log.info("Processing '" + query.getData() + "' callback with " + targetFeature.getName());
            targetFeature.processCallbackQuery(query);
        } else {
            log.info("Feature for processing " + query.getData() + " callback not found");
        }
    }

    public InlineKeyboardMarkup addCallbackMarking(Feature feature, InlineKeyboardMarkup replyMarkup) {
        for(List<InlineKeyboardButton> row : replyMarkup.getKeyboard()) {
            for (InlineKeyboardButton button : row) {
                button.setCallbackData(feature.getName() + button.getCallbackData());
            }
        }
        return replyMarkup;
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

    public boolean sendText(Feature feature, Message message, String text, boolean isReply) {
        return sendText(feature, message, text, isReply, null);
    }

    public boolean sendText(Feature feature, Message message, String text, boolean isReply, InlineKeyboardMarkup replyMarkup) {
        log.info("Sending message to \"{}\", text: \"{}\", is reply: {}", message.getFrom().getUserName(), text, isReply);
        SendMessage.SendMessageBuilder sendMessage = SendMessage.builder()
                .chatId(message.getChatId() + "")
                .text(text);
        if(isReply) {
            sendMessage.replyToMessageId(message.getMessageId());
        }
        if(replyMarkup != null) {
            addCallbackMarking(feature, replyMarkup);
            sendMessage.replyMarkup(replyMarkup);
        }
        try {
            execute(sendMessage.build());
            log.info("Message sent successfully");
            return true;
        } catch (TelegramApiException e) {
            log.info("Error sending message, "  + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean editText( Message message, String text, InlineKeyboardMarkup replyMarkup) {
        EditMessageText editText = EditMessageText.builder()
                .messageId(message.getMessageId())
                .chatId(message.getChatId().toString())
                .replyMarkup(message.getReplyMarkup())
                .text(text).build();
        try {
            execute(editText);
            log.info("Message edited successfully");
            return true;
        } catch (TelegramApiException e) {
            log.info("Error editing message, " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendPhoto(Feature feature, Message message, String link, boolean isReply, InlineKeyboardMarkup replyMarkup) {
        addCallbackMarking(feature, replyMarkup);
        SendPhoto.SendPhotoBuilder sendPhoto = SendPhoto.builder()
                .chatId(message.getChatId().toString())
                .photo(new InputFile(link))
                .replyMarkup(replyMarkup);
        if(isReply) {
            sendPhoto.replyToMessageId(message.getMessageId());
        }
        try {
            execute(sendPhoto.build());
            log.info("Photo sent successfully");
            return true;
        } catch (TelegramApiException e) {
            log.info("Error sending photo, "  + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String getBotUsername() {
        return System.getenv("TelegramBotUsername");
    }

    public String getToken() {
        return System.getenv("TelegramBotToken");
    }

}

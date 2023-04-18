package org.enkai.featurebot.features.googleimage;

import org.enkai.featurebot.core.Command;
import org.enkai.featurebot.core.Feature;
import org.enkai.featurebot.core.FeatureBot;
import org.enkai.featurebot.data.SimpleData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
//тут все треба нахуй переробити
public class GoogleImageFeature extends Feature {

    private final GoogleImageService imageService = new GoogleImageService();
    private final Logger log = LoggerFactory.getLogger(GoogleImageService.class);
    private final SimpleData db = SimpleData.getInstance();

    public GoogleImageFeature(FeatureBot bot) throws Exception {
        super(bot);
    }

    @Command("!.+")
    public void sendPhoto(Message message) {
        String query = message.getText().substring(1);
        List<String> links = imageService.getImages(query);

        User user = message.getFrom();
        log.info("Sending {} to \"{} {}\", for \"{}\"",links.get(0), user.getFirstName(), user.getUserName(), query);
        bot.sendPhoto(this, message, links.get(0), true, getKeyboard(links, 0));
    }

    private InlineKeyboardMarkup getKeyboard(List<String> links, int pos) {
        long id = System.nanoTime();
        storeLinks(id + "", links);
        log.info("Creating id: {}", id);
        return getKeyboard(links, pos, id);
    }

    private InlineKeyboardMarkup getKeyboard(List<String> links, int pos, long id) {
        log.info("Creating keyboard for {} position and {} id from {} images", pos, id, links.size());
        List<InlineKeyboardButton> row = new ArrayList<>();
        if(pos - 1 >= 0) {
            row.add(InlineKeyboardButton.builder().text("◀").callbackData(id + " " + (pos - 1) + " " + System.nanoTime()).build());
        } else {
            row.add(InlineKeyboardButton.builder().text("❌").callbackData("-❌").build());
        }
        row.add(InlineKeyboardButton.builder().text(pos + 1 + "/" + links.size()).callbackData("-" + (pos + 1)).build());
        if(pos + 1 < links.size()) {
            row.add(InlineKeyboardButton.builder().text("▶" ).callbackData(id + " " + (pos + 1) + " " + System.nanoTime()).build());
        } else {
            row.add(InlineKeyboardButton.builder().text("❌").callbackData("-❌").build());
        }
        return InlineKeyboardMarkup.builder().keyboardRow(row).build();
    }

    @Override
    public void processCallbackQuery(CallbackQuery query) {
        log.info("Processing query");
        String data = query.getData();
        if(data.startsWith("-")) {
            sendNotification(query, data.substring(1), false);
            return;
        }
        String[] parts = data.split(" ");
        long id = Long.parseLong(parts[0]);
        int pos = Integer.parseInt(parts[1]);
        List<String> links = getLinks(id + "");
        if(links == null) {
            sendNotification(query, "Image timeout", true);
            return;
        }
        EditMessageMedia media = EditMessageMedia.builder()
                .messageId(query.getMessage().getMessageId())
                .chatId(query.getMessage().getChatId().toString())
                .replyMarkup(bot.addCallbackMarking(this, getKeyboard(links, pos, id)))
                .media(InputMediaPhoto.builder().media(links.get(pos)).build())
                .build();
        while(true) {
            try {
                log.info("Sending {} to \"{} {}\"",media.getMedia().getMedia(), query.getFrom().getFirstName(), query.getFrom().getUserName());
                bot.execute(media);
                break;
            } catch (TelegramApiException e) {
                e.printStackTrace();
                log.info("Removing {}, now links contains {} images", links.remove(pos), links.size());
                media.setReplyMarkup(getKeyboard(links, pos, id));
                media.setMedia(InputMediaPhoto.builder().media(links.get(pos)).build());
            }
        }
    }

    private boolean sendNotification(CallbackQuery query, String text, boolean notification) {
        log.info("Sending notification to \"{} {}\" with text \"{}\"", query.getFrom().getFirstName(), query.getFrom().getUserName(), text);
        AnswerCallbackQuery answer = AnswerCallbackQuery.builder()
                .text(text)
                .callbackQueryId(query.getId())
                .showAlert(notification)
                .build();
        try {
            bot.execute(answer);
            return true;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void storeLinks(String key, List<String> links) {
        String data = "";
        for(String link : links) {
            data += link + " ";
        }
        db.put(key, data.trim());
    }

    private List<String> getLinks(String key) {
        String data = db.get(key);
        if(data == null) return null;
        List<String> links = new ArrayList<>();
        for(String link : data.split(" ")) {
            links.add(link);
        }
        return links;
    }

    @Override
    public String getName() {
        return "GoogleImageFeature";
    }
}

package org.enkai.featurebot.features.openai;

import org.enkai.featurebot.core.Command;
import org.enkai.featurebot.core.Feature;
import org.enkai.featurebot.core.FeatureBot;
import org.enkai.featurebot.data.SimpleData;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.enkai.featurebot.features.openai.objects.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OpenAiFeature extends Feature {

    private SimpleData db = SimpleData.getInstance();
    private OpenAiService openAiService = new OpenAiService();

    public OpenAiFeature(FeatureBot bot) {
        super(bot);
    }

    @Command("\\?.+")
    public void talk(Message message) throws Exception {
        message.setText("1" + message.getText());
        talkNTimes(message);
    }

    @Command("^[1-5]\\?.+")
    public void talkNTimes(Message message) throws Exception {
        String text = message.getText();
        int times = Integer.parseInt(text.charAt(0) + "");
        message.setText(message.getText().substring(2));
        ArrayList<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("user", message.getText()));
        if(message.isReply()) {
            messages.add(new ChatMessage(message.getReplyToMessage().getFrom().getIsBot() ? "assistant" : "user",message.getReplyToMessage().getText()));
        }
        if(!OpenAiService.Config.systemMessage.isBlank()) {
            messages.add(new ChatMessage("system", OpenAiService.Config.systemMessage));
        }
        Collections.reverse(messages);
        Response response = openAiService.send(messages, times);
        db.put("tokens", (Integer.parseInt(db.get("tokens")) + response.usage.total_tokens) + "");
        ArrayList<String> results = new ArrayList<>();
        for(Choice choice : response.choices) {
            results.add(choice.message.content);
        }
        if(results.size() == 1) {
            bot.sendText(this, message, results.get(0), true);
        } else {
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (int i = 0;i < results.size();i++) {
                long id = System.nanoTime();
                db.put(id + "", results.get(i));
                row.add(InlineKeyboardButton.builder().text((i + 1) + "").callbackData((i + 1) + "" + id).build());
            }
            bot.sendText(this, message, "№1\n" + results.get(0), message.isReply(), InlineKeyboardMarkup.builder().keyboardRow(row).build());
        }
    }

    @Command("/gettemperature")
    public void getTemp(Message message) {
        bot.sendText(this, message, OpenAiService.Config.temperature + "", true);
    }

    @Command("/t .+")
    public void setTemp(Message message) {
        String[] args = message.getText().split(" ");
        double temp = Double.parseDouble(args[1]);
        if(temp > 2 || temp < 0) {
            bot.sendText(this, message, "\uD83E\uDD28", true);
        } else {
            OpenAiService.Config.temperature = temp;
            bot.sendText(this, message, "Температура установлена", true);
        }
    }

    @Command("/clearsystem")
    public void clearSystem(Message message) {
        OpenAiService.Config.systemMessage = "";
        bot.sendText(this, message, "Системне повідомлення очищено", true);
    }

    @Command("/getsystem")
    public void showSys(Message message) {
        String system = OpenAiService.Config.systemMessage;
        if(system.isBlank()) {
            system = "Системне повідомлення пусте";
        }
        bot.sendText(this, message, system, true);
    }

    @Command("/s .+")
    public void setSys(Message message) {
        OpenAiService.Config.systemMessage = message.getText().substring(3);
        bot.sendText(this, message, "Системне повідомлення установлено", true);
    }


    @Command("/usage")
    public void tokenUsage(Message message) {
        int usedTokens = Integer.parseInt(db.get("tokens"));
        String answer = "Використано: " +  usedTokens + "/" + OpenAiService.Config.TOKEN_LIMIT + "\n";
        int percents = 100 * usedTokens / OpenAiService.Config.TOKEN_LIMIT;
        answer += percents + "% ";
        for(int i = 0;i < 25; i++) {
            answer += i * 4 < percents ? "|" : ".";
        }
        answer += " 100% " +  usedTokens / 500000f + "$";
        bot.sendText (this, message, answer, true);
    }

    @Override
    public void processCallbackQuery(CallbackQuery query) {
        String data = query.getData();
        char num = data.charAt(1);
        String text = db.get(data.substring(1));
        String sendText = "№" + num + "\n" + text;
        bot.editText(query.getMessage(), sendText, query.getMessage().getReplyMarkup());
    }

    @Override
    public String getName() {
        return "OpenAi";
    }
}

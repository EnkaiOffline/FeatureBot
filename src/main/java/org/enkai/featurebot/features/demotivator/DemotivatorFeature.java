package org.enkai.featurebot.features.demotivator;

import org.enkai.featurebot.core.Command;
import org.enkai.featurebot.core.Feature;
import org.enkai.featurebot.core.FeatureBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
//все нахуй переробляти треба
public class DemotivatorFeature extends Feature {

    private static Logger log = LoggerFactory.getLogger(DemotivatorFeature.class);

    public DemotivatorFeature(FeatureBot bot) {
        super(bot);
    }

    @Command("/dem .+")
    public void demotivate(Message message) {
        if(message.isReply() && message.getReplyToMessage().hasPhoto()) {
            List<PhotoSize> photos = message.getReplyToMessage().getPhoto();
            GetFile getFile = GetFile.builder().fileId(photos.get(photos.size() - 1).getFileId()).build();
            String text = message.getText().replace("/dem ", "");
            try {
                File file = bot.execute(getFile);
                String imageURL = file.getFileUrl(bot.getToken());
                URL url = new URL(imageURL);
                BufferedImage image = ImageIO.read(url);
                log.info("Demotivating {} with \"{}\"", imageURL, text);
                DemotivatorService demotivator = new DemotivatorService(image);
                image = demotivator.demotivate(text);
                log.info("Sending demotivated image to \"{} {}\"", message.getFrom().getFirstName(), message.getFrom().getUserName());
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", os);
                InputStream is = new ByteArrayInputStream(os.toByteArray());
                InputFile inputFile = new InputFile();
                inputFile.setMedia(is, "image");
                SendPhoto photo = SendPhoto.builder()
                        .chatId(message.getChatId() + "")
                        .replyToMessageId(message.getMessageId())
                        .photo(inputFile).build();
                bot.execute(photo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getName() {
        return "Demotivator";
    }
}

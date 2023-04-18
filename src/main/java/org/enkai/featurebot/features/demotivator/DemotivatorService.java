package org.enkai.featurebot.features.demotivator;

import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
//все нахуй переробляти треба
public class DemotivatorService {

    private static Logger log = LoggerFactory.getLogger(DemotivatorService.class);
    private static double averageWidth = 0.6;

    private BufferedImage src;
    private int innerBlack;
    private int innerWhite;
    private int blackTop;
    private int blackSide;
    private int textHeight;
    private int textDistance;

    public DemotivatorService(BufferedImage image) {
        log.info("Defining image parameters");
        src = image;
        innerBlack = Math.max(src.getHeight(), src.getWidth()) / 200;
        innerWhite = innerBlack;
        blackTop = Math.max(src.getHeight() / 10, src.getWidth() / 10);
        blackSide = src.getWidth() / 10;
        textHeight = Math.min(src.getHeight() / 8, src.getWidth() / 8);
        textDistance = textHeight / 4;
    }

    public BufferedImage demotivate(String text) {
        log.info("start");
        int maxSymbols = (src.getWidth() + innerWhite + innerBlack + blackSide) / (int)(textHeight * averageWidth);
        text = WordUtils.wrap(text, maxSymbols, null, true);
        String[] lines = text.split("\n");
        log.info(text + "\n" + lines.length);
        BufferedImage newImage = new BufferedImage(src.getWidth() + 2 * (innerBlack + innerWhite + blackSide),
                src.getHeight() + 2 * (innerBlack + innerWhite)  + 2 * blackTop + textHeight * lines.length + textDistance * (lines.length - 1), src.getType());
        Graphics graphics = newImage.createGraphics();
        drawInnerBorders(graphics);
        graphics.drawImage(src, blackSide + innerBlack + innerWhite, blackTop + innerBlack + innerWhite, null);
        drawText(graphics, lines);
        graphics.dispose();
        return newImage;
    }

    private void drawInnerBorders(Graphics graphics) {
        graphics.setColor(Color.WHITE);
        int borders = 2 * (innerBlack + innerWhite);
        graphics.fillRect(blackSide, blackTop, src.getWidth() + borders, src.getHeight() + borders);
        graphics.setColor(Color.BLACK);
        graphics.fillRect(blackSide + innerWhite, blackTop + innerWhite, src.getWidth() + 2 * innerBlack, src.getHeight() + 2 * innerBlack);
    }

    private void drawText(Graphics graphics, String[] lines) {
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("TimesRoman", Font.PLAIN, textHeight));
        for(int i = 0;i < lines.length;i++) {
            String line = lines[i];
            int textWidth = graphics.getFontMetrics().stringWidth(line);
            int borders = 2 * (innerBlack + innerWhite + blackTop / 2);
            graphics.drawString(line, (src.getWidth() - textWidth) / 2 + blackSide, src.getHeight() + borders + blackTop + i * (textHeight + textDistance));
        }
    }


}

package org.enkai.featurebot.features.googleimage;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GoogleImageService {

    private final String defaultImage;
    private final String errorImage;
    private final String userAgent;
    private final Pattern pattern;
    private final HttpClient client = HttpClient.newHttpClient();
    private final Logger log = LoggerFactory.getLogger(GoogleImageService.class);


    public GoogleImageService() throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getResourceAsStream("/gis.prefs"));
        defaultImage = properties.getProperty("defaultImage");
        errorImage = properties.getProperty("errorImage");
        userAgent = properties.getProperty("userAgent");
        pattern = Pattern.compile(properties.getProperty("pattern"));
    }

    public List<String> getImages(String query) {
        log.info("Searching images for \"{}\"", query);
        String googleQuery = createGoogleQuery(query);
        try {
            String html = getHTML(googleQuery);
            List<String> links = parseLinks(html);
            if(links.isEmpty()) {
                links.add(defaultImage);
            }
            log.info("Found {} images", links.size());
            return links;
        } catch (Exception e) {
            e.printStackTrace();
            return List.of(errorImage);
        }
    }

    private String createGoogleQuery(String query) {
        query = URLEncoder.encode(query);
        return "https://www.google.com/search?q=" + query + "&tbm=isch&nfpr=1";
    }

    private String getHTML(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .setHeader("User-Agent", userAgent)
                .uri(URI.create(url))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private List<String> parseLinks(String html) {
        List<Element> scripts = Jsoup.parse(html).getElementsByTag("script");
        List<String> result = new ArrayList<>();
        for(int i = 1;i <= 5;i++) {
            extractUrlsToList(scripts.get(scripts.size() - i).toString(), result);
        }
        return result.stream().filter(this::isImage).collect(Collectors.toList());
    }

    private boolean isImage(String link) {
        return link.endsWith(".jpg") || link.endsWith(".gif") || link.endsWith(".png") || link.endsWith(".jpeg");
    }

    private void extractUrlsToList(String input, List<String> result) {
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            result.add(matcher.group());
        }
    }

}

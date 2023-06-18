package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.DateTimeParser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HabrCareerParse implements Parse {
    private static final String SOURCE_LINK = "https://career.habr.com";
    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer", SOURCE_LINK);
    private static final Integer PAGE_NUMBER = 5;
    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    private static String retrieveDescription(String link) throws IOException {
        Connection connection = Jsoup.connect(link);
        Document document = connection.get();
        return document.select(".vacancy-description__text").text();
    }


    private Post createPost(String vacancyName, String linkString, LocalDateTime created, String description) {
        return new Post(vacancyName, linkString, created, description);
    }

    @Override
    public List<Post> list(String link) {
        List<Post> postList = new ArrayList<>();
        for (int i = 0; i < PAGE_NUMBER; i++) {
            Connection connection = Jsoup.connect(String.format("%s?page=%s", PAGE_LINK, i));
            try {
                Document document = connection.get();
                Elements rows = document.select(".vacancy-card__inner");
                rows.forEach(row -> {
                    Element titleElement = row.select(".vacancy-card__title").first();
                    String vacancyName = titleElement.text();
                    Element linkElement = titleElement.child(0);
                    String linkString = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                    Element dateElement = row.select(".vacancy-card__date").first();
                    Element dateTime = dateElement.child(0);
                    String date = dateTime.attr("datetime");
                    LocalDateTime created = dateTimeParser.parse(date);
                    String description = "";
                    try {
                        description = retrieveDescription(linkString);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    postList.add(createPost(vacancyName, linkString, created, description));
                });
                postList.forEach(System.out::println);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return postList;
    }
}
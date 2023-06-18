package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.util.List;
import java.util.StringJoiner;

public class HabrCareerParse {

    private static final String SOURCE_LINK = "https://career.habr.com";
    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer", SOURCE_LINK);
    private static final Integer PAGE_NUMBER = 5;

    public static void main(String[] args) throws IOException {
        for (int i = 0; i < PAGE_NUMBER; i++) {
            Connection connection = Jsoup.connect(String.format("%s?page=%s", PAGE_LINK, i));
            Document document = connection.get();
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                Element titleElement = row.select(".vacancy-card__title").first();
                String vacancyName = titleElement.text();
                Element linkElement = titleElement.child(0);
                String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                Element dateElement = row.select(".vacancy-card__date").first();
                Element dateTime = dateElement.child(0);
                String date = dateTime.attr("datetime");
                DateTimeParser dateParser = new HabrCareerDateTimeParser();
                String description = "";
                try {
                    description = retrieveDescription(link);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.printf("%s %s %s%n%s%n", vacancyName, link, dateParser.parse(date), description);
            });
            System.out.println("------------------");
        }
    }

    private static String retrieveDescription(String link) throws IOException {
        StringJoiner joiner = new StringJoiner(System.lineSeparator());
        Connection connection = Jsoup.connect(link);
        Document document = connection.get();
        List<String> rows = document.select(".vacancy-description__text").eachText();
        rows.forEach(joiner::add);
        return joiner.toString();
    }
}
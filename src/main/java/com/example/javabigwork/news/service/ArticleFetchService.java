package com.example.javabigwork.news.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleFetchService {
    private static final int MAX_CHARS = 5000;
    private static final int TIMEOUT_MS = 10000;

    public String fetchContent(String url) {
        if (url == null || url.isBlank()) return "";
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .referrer("https://news.google.com/")
                    .followRedirects(true)
                    .timeout(TIMEOUT_MS)
                    .get();

            doc.select("script, style, nav, header, footer, aside, .sidebar, .ad, .advertisement, .social-share, .related-articles, .comments, .comment, noscript").remove();

            String text = doc.select("article").text();
            if (text.length() < 100) text = doc.select("[itemprop=articleBody]").text();
            if (text.length() < 100) text = doc.select(".article-body, .story-body, .post-content, .entry-content, .article-content").text();
            if (text.length() < 100) text = doc.select("main").text();
            if (text.length() < 100) text = doc.select("[role=main]").text();
            if (text.length() < 100) {
                List<String> paragraphs = doc.select("p").eachText();
                paragraphs.removeIf(p -> p.length() < 30);
                text = String.join("\n\n", paragraphs);
            }
            if (text.length() < 50) text = doc.body().text();

            text = text.replaceAll("\\s+", " ").trim();
            if (text.length() > MAX_CHARS) {
                text = text.substring(0, MAX_CHARS) + "...";
            }
            return text;
        } catch (Exception e) {
            return "";
        }
    }
}

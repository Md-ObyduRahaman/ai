package com.claude.claudePractice.model;

import java.time.LocalDate;

public class BlogPost {

    private int id;
    private String title;
    private String content;
    private String excerpt;
    private String author;
    private String imageUrl;
    private LocalDate publishedDate;

    public BlogPost() {}

    public BlogPost(int id, String title, String content, String excerpt,
                    String author, String imageUrl, LocalDate publishedDate) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.excerpt = excerpt;
        this.author = author;
        this.imageUrl = imageUrl;
        this.publishedDate = publishedDate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public LocalDate getPublishedDate() { return publishedDate; }
    public void setPublishedDate(LocalDate publishedDate) { this.publishedDate = publishedDate; }
}

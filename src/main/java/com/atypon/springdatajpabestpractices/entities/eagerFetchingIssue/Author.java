package com.atypon.springdatajpabestpractices.entities.eagerFetchingIssue;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * BAD EXAMPLE: This entity uses EAGER fetching for both collections.
 *
 * Problems with EAGER fetching:
 * 1. Cartesian Product - Multiple EAGER collections cause duplicate rows
 * 2. Always loads data - Even when you only need the author's name
 * 3. Cannot be overridden - Unlike LAZY, you cannot make it lazy at query time
 * 4. Memory waste - Loads unnecessary data into memory
 */
@Entity
@Table(name = "author_eager")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String name;

    // BAD: EAGER fetching - always loads all books
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Book> books = new ArrayList<>();

    // BAD: Second EAGER collection - causes Cartesian product!
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Article> articles = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Book> getBooks() { return books; }
    public List<Article> getArticles() { return articles; }

    public void addBook(Book book) {
        books.add(book);
        book.setAuthor(this);
    }

    public void addArticle(Article article) {
        articles.add(article);
        article.setAuthor(this);
    }
}

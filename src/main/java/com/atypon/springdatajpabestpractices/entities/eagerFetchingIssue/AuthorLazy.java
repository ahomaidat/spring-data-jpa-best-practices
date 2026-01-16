package com.atypon.springdatajpabestpractices.entities.eagerFetchingIssue;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * GOOD EXAMPLE: This entity uses LAZY fetching (the default for collections).
 *
 * Benefits of LAZY fetching:
 * 1. Only loads data when accessed - no wasted queries
 * 2. Can be overridden with JOIN FETCH or EntityGraph when needed
 * 3. No Cartesian product issues
 * 4. Better memory usage
 */
@Entity
@Table(name = "author_lazy")
@NamedEntityGraph(
        name = "AuthorLazy.withBooks",
        attributeNodes = @NamedAttributeNode("books")
)
public class AuthorLazy {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String name;

    // GOOD: LAZY fetching (default) - only loads when needed
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookLazy> books = new ArrayList<>();

    // GOOD: LAZY fetching - can be loaded separately when needed
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArticleLazy> articles = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<BookLazy> getBooks() { return books; }
    public List<ArticleLazy> getArticles() { return articles; }

    public void addBook(BookLazy book) {
        books.add(book);
        book.setAuthor(this);
    }

    public void addArticle(ArticleLazy article) {
        articles.add(article);
        article.setAuthor(this);
    }
}

package com.atypon.springdatajpabestpractices.entities.eagerFetchingIssue;

import jakarta.persistence.*;

@Entity
@Table(name = "book_lazy")
public class BookLazy {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private AuthorLazy author;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public AuthorLazy getAuthor() { return author; }
    public void setAuthor(AuthorLazy author) { this.author = author; }
}

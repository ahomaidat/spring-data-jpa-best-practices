package com.atypon.springdatajpabestpractices.entities.NPlusOneIssue;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "post")
@NamedEntityGraph(
        name = "Post.withComments",
        attributeNodes = @NamedAttributeNode("comments")
)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String title;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostComment> comments = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<PostComment> getComments() { return comments; }

    public void addComment(PostComment comment) {
        comments.add(comment);
        comment.setPost(this);
    }
}
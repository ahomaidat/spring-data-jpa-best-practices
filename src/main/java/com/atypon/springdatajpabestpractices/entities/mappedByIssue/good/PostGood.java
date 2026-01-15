package com.atypon.springdatajpabestpractices.entities.mappedByIssue.good;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "post_good")
public class PostGood {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String title;

    //GOOD: mappedBy uses the FK in child table, no join table created! see: com.atypon.springdatajpabestpractices.MappedByTest
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostCommentGood> comments = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<PostCommentGood> getComments() {
        return comments;
    }

    public void addComment(PostCommentGood comment) {
        comments.add(comment);
        comment.setPost(this);
    }

    public void removeComment(PostCommentGood comment) {
        comments.remove(comment);
        comment.setPost(null);
    }
}
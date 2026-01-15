package com.atypon.springdatajpabestpractices.entities.bad;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "post_bad")
public class PostBad {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String title;

    //BAD: No mappedBy = Creates unnecessary JOIN TABLE. see  com.atypon.springdatajpabestpractices.MappedByTest
    @OneToMany(cascade = CascadeType.ALL)
    private List<PostCommentBad> comments = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<PostCommentBad> getComments() {
        return comments;
    }

    public void addComment(PostCommentBad comment) {
        comments.add(comment);
    }

    public void removeComment(PostCommentBad comment) {
        comments.remove(comment);
    }
}
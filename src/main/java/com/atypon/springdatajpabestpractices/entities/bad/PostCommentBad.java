package com.atypon.springdatajpabestpractices.entities.bad;

import jakarta.persistence.*;

@Entity
@Table(name = "post_comment_bad")
public class PostCommentBad {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String review;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setReview(String review) {
        this.review = review;
    }
}
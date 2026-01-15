package com.atypon.springdatajpabestpractices.entities.good;

import jakarta.persistence.*;

@Entity
@Table(name = "post_comment_good")
public class PostCommentGood {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private PostGood post;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public void setPost(PostGood post) {
        this.post = post;
    }
}
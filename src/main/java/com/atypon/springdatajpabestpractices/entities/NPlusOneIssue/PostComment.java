package com.atypon.springdatajpabestpractices.entities.NPlusOneIssue;


import jakarta.persistence.*;


@Entity
@Table(name = "post_comment")
public class PostComment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
}
package com.atypon.springdatajpabestpractices.repositroy.NPlusOneIssue;

import com.atypon.springdatajpabestpractices.entities.NPlusOneIssue.Post;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // BAD: This causes N+1 when you access comments
    List<Post> findAll();

    // GOOD: JOIN FETCH solution
    @Query("SELECT DISTINCT p FROM Post p JOIN FETCH p.comments")
    List<Post> findAllWithComments();

    // GOOD: EntityGraph solution using annotation
    @EntityGraph(attributePaths = "comments")
    @Query("SELECT p FROM Post p")
    List<Post> findAllWithCommentsGraph();

    // GOOD: Using @NamedEntityGraph defined on entity
    @EntityGraph(value = "Post.withComments")
    @Query("SELECT p FROM Post p")
    List<Post> findAllWithCommentsNamedGraph();
}
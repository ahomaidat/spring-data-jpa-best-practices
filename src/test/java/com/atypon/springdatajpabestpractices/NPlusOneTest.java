package com.atypon.springdatajpabestpractices;

import com.atypon.springdatajpabestpractices.entities.NPlusOneIssue.*;
import com.atypon.springdatajpabestpractices.repositroy.NPlusOneIssue.PostRepository;
import com.atypon.springdatajpabestpractices.util.SQLInterceptor;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * N+1 Problem: When fetching N parent entities, Hibernate executes
 * 1 query for parents + N queries for children = N+1 total queries.
 *
 * With 1000 posts, that's 1001 queries instead of 1!
 */
@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NPlusOneTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private PostRepository postRepository;

    @BeforeEach
    void setup() {
        for (int i = 1; i <= 5; i++) {
            Post post = new Post();
            post.setTitle("Post " + i);
            for (int j = 1; j <= 3; j++) {
                PostComment comment = new PostComment();
                comment.setReview("Comment " + j + " on Post " + i);
                post.addComment(comment);
            }
            em.persist(post);
        }
        em.flush();
        em.clear();
        SQLInterceptor.clear();
    }

    @Test
    @Order(1)
    @DisplayName("BAD: N+1 Problem - findAll() triggers extra queries")
    void nPlusOneProblem() {
        SQLInterceptor.clear();

        // Uses: List<Post> findAll();
        List<Post> posts = postRepository.findAll();

        for (Post post : posts) {
            post.getComments().size();
        }

        SQLInterceptor.printQueries("BAD: N+1 Problem with findAll()");

        int selectCount = (int) SQLInterceptor.getQueries().stream()
                .filter(q -> q.startsWith("select"))
                .count();

        System.out.println("We fetched " + posts.size() + " posts");
        System.out.println("Total SELECT queries: " + selectCount + " (1 + " + posts.size() + " = N+1)");
        System.out.println("\nWith 1000 posts, this would be 1001 queries!\n");

        assertEquals(6, selectCount, "N+1: Expected 1 + 5 = 6 queries");
    }

    @Test
    @Order(2)
    @DisplayName("GOOD: JOIN FETCH with @Query annotation")
    void joinFetchSolution() {
        SQLInterceptor.clear();

        // Uses: @Query("SELECT DISTINCT p FROM Post p JOIN FETCH p.comments")
        List<Post> posts = postRepository.findAllWithComments();

        for (Post post : posts) {
            post.getComments().size();
        }

        SQLInterceptor.printQueries("GOOD: JOIN FETCH with @Query");

        int selectCount = (int) SQLInterceptor.getQueries().stream()
                .filter(q -> q.startsWith("select"))
                .count();

        System.out.println("We fetched " + posts.size() + " posts with all comments");
        System.out.println("Total SELECT queries: " + selectCount + "\n");

        assertEquals(1, selectCount, "JOIN FETCH: Expected only 1 query");
    }

    @Test
    @Order(3)
    @DisplayName("GOOD: @EntityGraph with attributePaths")
    void entityGraphSolution() {
        SQLInterceptor.clear();

        // Uses: @EntityGraph(attributePaths = "comments")
        List<Post> posts = postRepository.findAllWithCommentsGraph();

        for (Post post : posts) {
            post.getComments().size();
        }

        SQLInterceptor.printQueries("GOOD: @EntityGraph(attributePaths)");

        int selectCount = (int) SQLInterceptor.getQueries().stream()
                .filter(q -> q.startsWith("select"))
                .count();

        System.out.println("We fetched " + posts.size() + " posts with all comments");
        System.out.println("Total SELECT queries: " + selectCount + "\n");

        assertEquals(1, selectCount, "EntityGraph: Expected only 1 query");
    }

    @Test
    @Order(4)
    @DisplayName("GOOD: @NamedEntityGraph defined on entity")
    void namedEntityGraphSolution() {
        SQLInterceptor.clear();

        // Uses: @EntityGraph(value = "Post.withComments")
        List<Post> posts = postRepository.findAllWithCommentsNamedGraph();

        for (Post post : posts) {
            post.getComments().size();
        }

        SQLInterceptor.printQueries("GOOD: @NamedEntityGraph on entity");

        int selectCount = (int) SQLInterceptor.getQueries().stream()
                .filter(q -> q.startsWith("select"))
                .count();

        System.out.println("We fetched " + posts.size() + " posts with all comments");
        System.out.println("Total SELECT queries: " + selectCount + "\n");

        assertEquals(1, selectCount, "NamedEntityGraph: Expected only 1 query");
    }

    @Test
    @Order(5)
    @DisplayName("SUMMARY: N+1 Problem and Solutions")
    void summary() {
        System.out.println("""
            
            WHAT IS THE N+1 PROBLEM?
            ------------------------
            
            When you load a list of entities and access their lazy associations,
            Hibernate executes:
            
              - 1 query to load the parents (Posts)
              - N queries to load each parent's children (Comments)
              
            Total: N+1 queries. With 10,000 posts = 10,001 queries!
            
            
            SOLUTION 1: JOIN FETCH with @Query
            ----------------------------------
            
            @Query("SELECT DISTINCT p FROM Post p JOIN FETCH p.comments")
            List<Post> findAllWithComments();
            
            
            SOLUTION 2: @EntityGraph with attributePaths
            --------------------------------------------
            
            @EntityGraph(attributePaths = "comments")
            @Query("SELECT p FROM Post p")
            List<Post> findAllWithCommentsGraph();
            
            
            SOLUTION 3: @NamedEntityGraph on entity
            ---------------------------------------
            
            On the entity class:
            
            @Entity
            @NamedEntityGraph(
                name = "Post.withComments",
                attributeNodes = @NamedAttributeNode("comments")
            )
            public class Post { ... }
            
            In repository:
            
            @EntityGraph(value = "Post.withComments")
            @Query("SELECT p FROM Post p")
            List<Post> findAllWithCommentsNamedGraph();
            
            
            WHEN TO USE WHAT?
            -----------------
            
            - JOIN FETCH: Simple, explicit, works everywhere
            - @EntityGraph: Cleaner, reusable, good for Spring Data JPA
            - @NamedEntityGraph: Best for complex graphs used in multiple places
            
            """);
    }
}
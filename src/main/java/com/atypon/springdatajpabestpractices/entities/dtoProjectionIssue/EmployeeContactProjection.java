package com.atypon.springdatajpabestpractices.entities.dtoProjectionIssue;

/**
 * Interface projection for contact information only.
 *
 * Example: Building a contact list UI that only shows name, email, phone.
 * No need to load salary, biography, profile picture, etc.
 */
public interface EmployeeContactProjection {

    Long getId();
    String getFirstName();
    String getLastName();
    String getEmail();
    String getPhone();
}

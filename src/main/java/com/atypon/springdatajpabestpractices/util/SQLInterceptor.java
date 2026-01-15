package com.atypon.springdatajpabestpractices.util;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class SQLInterceptor implements StatementInspector {

    private static final List<String> queries = Collections.synchronizedList(new ArrayList<>());

    @Override
    public String inspect(String sql) {
        queries.add(sql);
        return sql;
    }

    public static void clear() {
        queries.clear();
    }

    public static List<String> getQueries() {
        return new ArrayList<>(queries);
    }

    public static int getInsertCount() {
        return (int) queries.stream().filter(q -> q.startsWith("insert")).count();
    }

    public static int getDeleteCount() {
        return (int) queries.stream().filter(q -> q.startsWith("delete")).count();
    }

    public static void printQueries(String header) {
        System.out.println("\n" + header);
        System.out.println("-".repeat(70));
        for (int i = 0; i < queries.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + queries.get(i));
        }
        System.out.println("-".repeat(70));
        System.out.println("Inserts: " + getInsertCount() + " | Deletes: " + getDeleteCount() + "\n");
    }
}
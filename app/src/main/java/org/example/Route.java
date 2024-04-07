package org.example;

import java.util.Objects;

public class Route {
    String method;
    String path;

    Route(String method, String path) {
        this.method = method;
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route route = (Route) o;
        return Objects.equals(method, route.method) && Objects.equals(path, route.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, path);
    }
}
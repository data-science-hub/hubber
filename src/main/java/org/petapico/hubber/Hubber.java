package org.petapico.hubber;

import static spark.Spark.*;

public class Hubber {
    public static void main(String[] args) {
        get("/hello", (req, res) -> "Hello World");
    }
}
# Cafe Management System

A Java-based cafe management web application built with Javalin, a custom SQL-in-Java ORM, and SQLite.

## Architecture

- **Backend**: Java 19, Javalin 6 web framework, running on port 5000
- **Database**: SQLite (`cafe.db`) via the custom `sql_in_java` ORM
- **Frontend**: Vanilla JS/HTML served as static files from `/public`
- **Build**: Maven (mvn package produces `target/sql-in-java-1.0.0.jar`)

## Project Structure

- `src/main/java/app/` — Application layer (Main, Database, StudDB, models)
- `src/main/java/sql_in_java/` — Custom ORM and SQL DSL library
- `src/main/resources/public/` — Static frontend files (HTML/CSS/JS)
- `pom.xml` — Maven build configuration

## Running

The "Start application" workflow builds and runs the app:
```
mvn package -q -DskipTests && java -jar target/sql-in-java-1.0.0.jar
```

## Credentials

- **Admin login**: password `admin123`
- **Customer login**: requires a valid customer ID in the database

## Key Features

- Customer: browse menu, place orders (cash/card/online), view order history
- Admin: dashboard stats, manage orders, manage products, view inventory, sales reports

## User Preferences

- Uses SQLite (not MySQL) for local development on Replit
- Port 5000 for the web server

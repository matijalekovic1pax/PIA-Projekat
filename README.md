# Coworking Hub Manager

Full-stack web application: **Angular 21** frontend + **Spring Boot 3** backend + **MySQL** database.

---

## Quick Start

### 1. Set Up MySQL Database

```bash
mysql -u root -p < database.sql
```

Creates `coworking_db` with all tables.

> Edit `backend/src/main/resources/application.properties` if your MySQL password is not `root`.

### 2. Start the Spring Boot Backend

```bash
cd backend
mvn spring-boot:run
```

Backend runs on **http://localhost:8080** — requires Java 17+ and Maven 3.6+

### 3. Start the Angular Frontend

```bash
npm install
ng serve
```

Frontend runs on **http://localhost:4200** — requires Node.js 18+ and Angular CLI

### 4. Create Admin User

After starting the backend, activate the seeded admin account:

```sql
USE coworking_db;
UPDATE users SET status='ACTIVE' WHERE username='admin';
-- Default password is: admin123
```

Then log in at `http://localhost:4200/admin-login`

---

## Project Structure

```
PIA-Projekat/       ← Angular frontend (this folder)
backend/            ← Spring Boot backend
database.sql        ← MySQL setup script
```

---

# Angular CLI (auto-generated docs below)

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 21.1.2.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.

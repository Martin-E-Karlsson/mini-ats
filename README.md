# Mini-ATS

A small applicant tracking system (ATS) for recruiters. Clients ("customers") post the jobs they're hiring for, add candidates with their profile details, and work those candidates through a hiring pipeline on a drag-and-drop kanban board. Admins can do everything customers can, on their behalf, and manage user accounts. It also includes an optional AI feature that screens a candidate's CV against a job.

Built as a Java / Spring Boot application that uses **Supabase** for authentication, database, and file storage.

## Features

- **Authentication** via Supabase Auth (email + password). No public sign-up — accounts are created by admins.
- **Roles**: `admin` and `customer`. Customers only ever see and edit their own jobs and candidates; admins see and manage everything. Authorization is enforced in the service layer.
- **Jobs**: customers create and manage the roles they're recruiting for.
- **Candidates**: added with name, email, LinkedIn URL, notes, and an optional uploaded CV.
- **Kanban board**: candidates flow through stages (`applied → screening → interview → offer → hired → rejected`), by drag-and-drop or with per-card advance/back buttons. Filter by job and by candidate name.
- **User management**: admins list all accounts, create admin/customer accounts, edit any user (name, email, role, password), and delete accounts.
- **Profile**: every user can edit their own name, email, and password (but not their own role).
- **CV upload**: CVs are stored in Supabase Storage; the candidate's CV pre-fills the screening tool.
- **AI CV screening**: sends the CV text and the job context to an LLM (Anthropic) for a short fit assessment. Works without an API key by explaining the approach instead.

## Tech stack

- Java 26, Spring Boot 4.1 (Web MVC, Spring Security, Data JPA)
- Thymeleaf server-rendered UI
- Supabase: Auth (GoTrue), PostgreSQL, Storage
- Apache PDFBox (CV text extraction)
- Anthropic API (optional, for AI screening)
- Maven, JUnit 5 + Mockito

## Architecture in one line

The browser talks to **Spring Boot**, which holds all business logic and authorization. Spring delegates **authentication** to Supabase Auth (verifying passwords, creating accounts via the Admin API) and connects to **Supabase Postgres** over JDBC. Because it connects as the database owner, row-level security is bypassed and ownership rules are enforced in Java.

## Prerequisites

- JDK 26
- A free [Supabase](https://supabase.com) project
- (Optional) An [Anthropic API key](https://platform.claude.com) for live AI screening
- (Optional) Docker, if you want to build/run the container

## Getting it running

### 1. Set up Supabase

1. Create a new Supabase project. Save the **database password** you set.
2. **SQL Editor** → paste and run [`db/schema.sql`](db/schema.sql). This creates the `profiles`, `jobs`, and `candidates` tables, the role model, the auto-profile trigger, and RLS policies.
3. **Storage** → create a new bucket named exactly **`cvs`**, set to **Private**.
4. Create your first admin user:
   - **Authentication → Users → Add user** — enter an email and password, and tick **Auto Confirm User**.
   - **SQL Editor** → promote it:
     ```sql
     update public.profiles set role = 'admin' where email = 'you@example.com';
     ```
5. Collect the connection details:
   - **Settings → API**: Project URL, the **publishable** key, the **secret** key, and the JWKS URL (`<project-url>/auth/v1/.well-known/jwks.json`).
   - **Connect → Transaction pooler**: the host (e.g. `aws-1-eu-north-1.pooler.supabase.com`) and username (`postgres.<project-ref>`).

### 2. Configure the app

Create a `.env` file in the project root (it's gitignored) with your secrets:

```properties
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_xxx
SUPABASE_SECRET_KEY=sb_secret_xxx
SUPABASE_JWKS_URL=https://<project-ref>.supabase.co/auth/v1/.well-known/jwks.json
SUPABASE_DB_PASSWORD=your-database-password
# Optional — leave unset to run AI screening in "explain the approach" mode:
ANTHROPIC_API_KEY=
```

The non-secret database **host** and **username** live in `src/main/resources/application.properties`
(`spring.datasource.url` and `spring.datasource.username`). If you're pointing at a different
Supabase project than the one this was built against, update those two values to match step 1.

### 3. Run it

```bash
./mvnw spring-boot:run
```

Then open <http://localhost:8080> and sign in with the admin account you created. From the
**Users** menu you can create more accounts; everything else flows from the **Jobs** and **Board** pages.

### 4. Run the tests

```bash
./mvnw test
```

The suite is unit/slice tests (Mockito + Spring MockMvc) and needs no database or network.

## Environment variables

| Variable | Required | Description |
|---|---|---|
| `SUPABASE_URL` | yes | Your project URL, `https://<ref>.supabase.co` |
| `SUPABASE_PUBLISHABLE_KEY` | yes | Public (anon-equivalent) API key |
| `SUPABASE_SECRET_KEY` | yes | Secret (service-role-equivalent) API key — server-side only |
| `SUPABASE_JWKS_URL` | yes | JWKS endpoint for validating Supabase tokens |
| `SUPABASE_DB_PASSWORD` | yes | Database password (used for the JDBC connection) |
| `ANTHROPIC_API_KEY` | no | Enables live AI CV screening |
| `PORT` | no | Server port (set automatically by hosts like Render; defaults to 8080) |

## Deployment

The app ships with a `Dockerfile` (multi-stage build → JRE runtime) and a `render.yaml` blueprint.
On a host like Render, deploy as a **Docker** web service, set the environment variables above in the
dashboard, and point it at the same Supabase project. Note that free tiers sleep when idle, and a free
Supabase project pauses after 7 days of inactivity.

## Project structure

```
src/main/java/com/mek/miniats
├── auth/         Supabase Auth integration + Spring Security provider
├── admin/        account management (create/edit/delete users)
├── user/         profiles, roles, self-service profile editing
├── job/          jobs domain (entity, repository, service, controller)
├── candidate/    candidates + kanban (CRUD, stage moves, CV upload)
├── board/        the kanban board view
├── screening/    AI CV screening (LLM client, service, controller)
├── storage/      Supabase Storage client + CV text extraction
├── common/       shared helpers and exceptions
└── config/       Spring Security configuration
db/schema.sql     database schema to run in Supabase
```

# Bederer AI

A tennis swing coaching app: upload a video, get an AI-generated technique
breakdown (stance, backswing, contact point, follow-through, footwork), and
track your scores over time.

This is a rebuild of an existing personal project (originally a single
Next.js app) split into a Spring Boot backend + React frontend, meant to
demonstrate the stack in a specific job posting (Spring Boot, Spring
Security, REST APIs, React with hooks, JPA/Hibernate, SQL database,
JUnit/Mockito, Jest, Docker, CI/CD) - and meant to actually be deployed so a
recruiter can use it live, not just read the code.

## What it does

Record 5-10 seconds of a tennis swing, upload it, pick the shot type
(forehand/backhand/serve/volley). The backend extracts 6 frames from the
video with ffmpeg, sends them to Claude's vision API with a coaching prompt,
and returns a scored breakdown per category plus a top-priority fix and a
drill recommendation. Past analyses are saved to Postgres and shown in a
"My Swings" tab with a score-over-time chart.

## Stack

**Backend** - Java 11, Spring Boot 2.7 (Web, Data JPA, Security, Validation),
Hibernate, H2 (dev) / PostgreSQL (prod), ffmpeg (shelled out via
`ProcessBuilder`), Claude API (called directly via `java.net.http.HttpClient`
+ Jackson, no extra SDK dependency), Maven, JUnit 5 + Mockito + MockMvc.

**Frontend** - React 18 (hooks, Context API for auth), Tailwind CSS, recharts,
Vite, Jest + React Testing Library.

**Infra** - Dockerfiles for both services (backend's installs ffmpeg at the
system level, not a JVM library), docker-compose (Postgres + backend +
frontend/nginx), GitHub Actions CI.

## Why these choices

Reads (`GET /api/swings`) are public so a recruiter can browse the history
tab without logging in. Writes (`POST`/`DELETE`) require a single shared
demo login (HTTP Basic) - since every analysis costs a real Anthropic API
call plus ffmpeg CPU time, this isn't gated behind full user accounts, just
one login you hand out alongside the URL. On top of that, there's a
`RateLimiterService` capping analyses to 20/day (configurable) regardless of
who's logged in, as a hard ceiling against runaway API costs. Frame images
are only ever returned in the response immediately after an upload, never
persisted to the database - same behavior as the original app, which also
never stored images past the live session.

## Running it locally

### Backend

```bash
cd backend
export ANTHROPIC_API_KEY=sk-ant-...
mvn spring-boot:run
```

Starts on `http://localhost:8080` with H2 in-memory (no setup needed).
Demo login defaults to `demo` / `changeme` unless you set `DEMO_USERNAME`
/ `DEMO_PASSWORD`.

Run tests (mocked ffmpeg/Claude calls, no real API key needed):

```bash
mvn test
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Starts on `http://localhost:5173`, proxies `/api` to the backend.

Run tests:

```bash
npm test
```

### Everything via Docker

```bash
export ANTHROPIC_API_KEY=sk-ant-...
export DEMO_PASSWORD=some-real-password
docker compose up --build
```

Frontend on `http://localhost:3000` (nginx, proxies `/api` to the backend
container), backend on `http://localhost:8080`, Postgres behind both.

## API

| Method | Path                | Auth       | Description                          |
|--------|---------------------|------------|---------------------------------------|
| POST   | `/api/swings`       | demo login | Upload video (multipart), get analysis |
| GET    | `/api/swings`       | public     | Last 10 analyses                     |
| GET    | `/api/swings/{id}`  | public     | One analysis                         |
| DELETE | `/api/swings/{id}`  | demo login | Delete an analysis                   |

## Deploying to Render (the path actually used)

**Update:** the AWS path below (ECS + RDS IAM auth) is fully implemented in
the code and was a real, working exercise in IAM roles, task definitions,
and RDS auth - but it's not what's actually live. This deployed to
**Render** instead, using the `render.yaml` Blueprint at the repo root,
which provisions all three pieces (Postgres, backend, frontend) from one
file instead of clicking through a dashboard three separate times.

1. **Push this repo to GitHub** (Render Blueprints deploy from a connected
   git repo, not a local folder):
   ```bash
   git remote add origin https://github.com/<your-username>/bederer-ai-app.git
   git branch -M main
   git push -u origin main
   ```

2. **In the Render dashboard:** "New" -> "Blueprint" -> connect the repo.
   Render finds `render.yaml` automatically and shows a preview of the three
   resources it's about to create (`bedererai-db`, `bedererai-backend`,
   `bedererai-frontend`). Click through to create them.

3. **Fill in the secrets.** `render.yaml` intentionally leaves
   `ANTHROPIC_API_KEY` and `DEMO_PASSWORD` blank (`sync: false`) rather than
   committing real secrets to git - after the first sync, go to
   `bedererai-backend` -> Environment and set both values by hand.

4. **Wait for both services to build.** The backend build installs ffmpeg
   in the Docker image (same Dockerfile as the AWS path) and can take a few
   minutes; the frontend is a static build and is much faster.

5. **Done** - `https://bedererai-frontend.onrender.com` is the URL to hand a
   recruiter, with the demo credentials you set in step 3.

### Why this was simpler than the AWS path

No task roles, no execution roles, no IAM auth token refresh, no matching
region strings across four different resources, no separate ECR push step -
`render.yaml` reads directly from the repo, builds the Dockerfile itself,
and wires the three services together using the `fromDatabase` binding
instead of manually copying connection strings between dashboard tabs. The
one real wrinkle: Render's Postgres only exposes a single connection string
(`postgres://user:password@host:port/database`), not separate host/port
fields the way RDS effectively did - so
`RenderDatabaseUrlEnvironmentPostProcessor` (in `backend/src/main/java/.../config/`)
parses that at startup into the `jdbc:postgresql://...` URL plus separate
username/password Spring Boot's autoconfiguration expects. It only runs when
`DATABASE_URL` is in Render's `postgres://` form and has zero effect on the
AWS/IAM path, so both deployment targets work from the same codebase.

### Free-tier caveats worth knowing

Render's free Postgres plan expires 30 days after creation and gets deleted
- fine for demoing this over the next few weeks, but worth upgrading the
plan (or exporting the data) before then if this needs to stay up longer.
The free backend web service also spins down after 15 minutes of no
traffic and takes 30-50 seconds to cold-start on the next request - if a
recruiter's first click hangs for a bit before the page loads, that's why.

## Deploying to AWS (built, not currently live)

This path is fully implemented (`RdsIamDataSourceConfig`, the `rds-db:connect`
IAM policy, ECS task definition shape) and worth knowing how to describe in
an interview even though Render is what's actually running. **Note:** App
Runner stopped accepting new customers, so this deploys on
**ECS (Fargate) + RDS Postgres with IAM database auth**, fronted by **S3 +
CloudFront** for the static frontend build. IAM auth means the ECS task
authenticates to RDS with a short-lived token generated from its own IAM
role, instead of a long-lived database password sitting in an environment
variable - one less secret to leak.

1. **RDS Postgres**, IAM auth enabled. When creating the instance, turn on
   "Password and IAM database authentication" under Database authentication.
   Note the endpoint, port, and database name.

2. **Create an IAM-mapped database user.** Connect to the instance once with
   the master password and run:
   ```sql
   CREATE USER bedererai WITH LOGIN;
   GRANT rds_iam TO bedererai;
   GRANT ALL PRIVILEGES ON DATABASE bedererai TO bedererai;
   ```
   This user has no password at all - RDS accepts the IAM auth token in its
   place.

3. **ECS task role policy.** Attach a policy granting the task role
   permission to generate connection tokens for that specific user:
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": "rds-db:connect",
         "Resource": "arn:aws:rds-db:<region>:<account-id>:dbuser:<resource-id>/bedererai"
       }
     ]
   }
   ```
   `<resource-id>` is the RDS instance's resource ID (starts with `db-`),
   visible in the RDS console under Configuration - not the instance name.

4. **ECR + build/push the backend image:**
   ```bash
   docker build -t bederer-ai-backend ./backend
   aws ecr get-login-password | docker login --username AWS --password-stdin <your-ecr-url>
   docker tag bederer-ai-backend:latest <your-ecr-url>/bederer-ai-backend:latest
   docker push <your-ecr-url>/bederer-ai-backend:latest
   ```

5. **ECS task definition.** Point it at that ECR image, attach the task role
   from step 3, and set these environment variables:
   - `SPRING_PROFILES_ACTIVE=prod`
   - `DATASOURCE_IAM_AUTH=true` (this is what switches the app from a static
     password to `RdsIamDataSourceConfig` - see that class for how the token
     refresh works)
   - `DATABASE_HOST=<rds-endpoint>`, `DATABASE_PORT=5432`,
     `DATABASE_NAME=bedererai`, `DATABASE_USERNAME=bedererai`
   - `AWS_REGION=<region>` (must match the region in the task role policy's ARN)
   - `ANTHROPIC_API_KEY`
   - `DEMO_USERNAME`, `DEMO_PASSWORD` (the credentials you'll hand a recruiter)
   - `ALLOWED_ORIGINS=https://<your-cloudfront-domain>` (fill in after step 7)
   - `MAX_ANALYSES_PER_DAY` (optional, defaults to 20)

   Note there's no `DATABASE_PASSWORD` anywhere in this list - that's the
   point of IAM auth.

6. **Security group.** The RDS instance's security group needs to allow
   inbound Postgres (5432) from the ECS task's security group specifically,
   not from your ECS cluster's default outbound-open setup - IAM auth
   controls *who* can authenticate, not *whether the network connection is
   allowed to be attempted* in the first place.

7. **Frontend: build against the backend's URL, then S3 + CloudFront:**
   ```bash
   cd frontend
   VITE_API_BASE_URL=https://<your-backend-url>/api npm run build
   ```
   Upload `frontend/dist` to an S3 bucket, put CloudFront in front of it.
   Then go back to step 5 and set `ALLOWED_ORIGINS` to the CloudFront
   domain - without it, the browser blocks the frontend's requests to the
   backend with a CORS error.

8. **Hand out the URL + demo credentials** to the recruiter.
   `MAX_ANALYSES_PER_DAY` caps usage so a shared link can't run up an
   unbounded Anthropic bill.

### Why IAM auth instead of a database password

It's a real security improvement (no long-lived DB credential sitting in an
env var or Secrets Manager to rotate/leak), but it's also just a stronger
thing to say happened in an interview than "I put a password in an
environment variable." The tradeoff is real complexity: `RdsIamDataSourceConfig`
exists specifically because Spring Boot's default `DataSourceAutoConfiguration`
has no concept of a password that expires and needs refreshing mid-flight -
that's the one piece of this deployment that couldn't just be a config
change.

### ffmpeg in the container

Note the backend Dockerfile installs `ffmpeg` via `apt-get` in the final
image stage - it's a system binary, not a JVM library, so this is the one
real infra wrinkle versus a typical Spring Boot deployment.

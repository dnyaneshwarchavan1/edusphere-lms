# EduSphere LMS Microservices Blueprint

This repository currently runs as a single Spring Boot LMS. The files in this folder define the next migration path toward a production-style architecture without breaking the working app.

## Recommended storage

| Requirement | Best Option |
| --- | --- |
| Full Production LMS | AWS S3 |
| Free Beginner Setup | Cloudinary |
| Self Hosted | MinIO |
| Image Optimization | Cloudinary |
| Kubernetes Ready | MinIO |
| Large Video Streaming | AWS S3 |

## Target architecture

```text
React Frontend
      |
      v
API Gateway
      |
      v
------------------------------------------------
| Auth Service                                |
| Course Service                              |
| Payment Service                             |
| Chat Service                                |
| Notification Service                        |
------------------------------------------------
      |
      v
MySQL / Redis / Kafka / Cloudinary
```

## Separate databases

```text
auth_db
course_db
payment_db
```

## Step-by-step migration plan

### Step 1: Storage provider abstraction

Status in this repo:
- complete
- local storage still works
- Cloudinary can be enabled with env vars

Used for:
- assignment reference files
- assignment submission files

### Step 2: Split the core services

Initial service boundaries:

#### Auth Service
- registration
- login
- JWT generation
- user roles
- user profile

Database:
- `auth_db`

#### Course Service
- courses
- modules
- lessons
- enrollments
- certificates
- assignments
- quizzes

Database:
- `course_db`

#### Payment Service
- checkout
- gateway verification
- payment history
- revenue reporting

Database:
- `payment_db`

### Step 3: Production support services

#### API Gateway
- single frontend entry point
- routing to auth, course, payment, chat, notification

#### Redis
- caching
- sessions if needed later
- notification/chat presence

#### Kafka
- async events such as:
  - payment success
  - enrollment created
  - notification broadcast
  - course approved

#### Cloudinary
- image and document storage

## Suggested migration order

1. Auth service
2. Course service
3. Payment service
4. Gateway
5. Redis
6. Kafka
7. Chat and notification extraction

## Repo scaffolding

See:
- `ops/docker-compose.microservices.yml`
- `microservices/auth-service/application.yml`
- `microservices/course-service/application.yml`
- `microservices/payment-service/application.yml`
- `microservices/api-gateway/application.yml`

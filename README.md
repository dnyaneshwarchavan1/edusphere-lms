# EduSphere LMS

EduSphere LMS is a resume-ready full-stack Learning Management System built with **Java, Spring Boot, Hibernate/JPA, Spring Security, MySQL, React, Bootstrap, and JWT authentication**.

## Live Deployment Plan

- Frontend: Vercel free Hobby plan
- Backend: Render free web service
- Database: MySQL locally, or a managed MySQL-compatible cloud database for deployment

## Features

- JWT login and registration
- BCrypt password encryption
- Role-based dashboards for Admin, Instructor, and Student
- Course catalog and course details
- Instructor course creation
- Admin course approval
- Student enrollment
- Lesson completion and progress tracking
- Quiz creation and quiz attempt APIs
- Seeded demo users and seeded course data
- Swagger UI API docs
- Dockerfile for backend deployment
- Vercel config for React routing
- Provider-based assignment file storage with local storage or Cloudinary
- Microservices architecture blueprint for auth, course, payment, gateway, Redis, and Kafka

## Tech Stack

### Backend

- Java 21
- Spring Boot 3.3
- Spring Web
- Spring Data JPA
- Hibernate
- Spring Security
- JWT
- MySQL
- Maven
- Swagger/OpenAPI
- Cloudinary-ready upload integration

### Frontend

- React.js
- Vite
- Bootstrap 5
- JavaScript
- Axios
- React Router
- Lucide React
- Recharts

## Folder Structure

```text
edusphere-lms/
  backend/
  frontend/
  render.yaml
  README.md
```

## Demo Login Accounts

After starting the backend, these users are created automatically:

```text
Admin:
admin@edusphere.com
password

Instructor:
instructor@edusphere.com
password

Student:
student@edusphere.com
password
```

## Run Locally

You can use the included Windows helper scripts from the project root:

```text
mvnw.cmd
run-project.bat
build-backend.bat
run-backend.bat
run-frontend.bat
```

Easy option:

```text
run-project.bat
```

This opens the backend first. After the backend is fully started, run `run-frontend.bat`.

Manual option:

1. Run `run-backend.bat` and keep that terminal open.
2. Wait until the backend terminal shows `Tomcat started on port 8080`.
3. Run `run-frontend.bat` and keep that terminal open.
4. Open `http://localhost:5173`.

The backend can take 40-60 seconds to start the first time. Wait until you see a message like:

```text
Tomcat started on port 8080
Started LmsApplication
```

If `run-frontend.bat` says `npm is not recognized`, install Node.js. If `run-backend.bat` says `java is not recognized`, install Java 21 or newer.

### Backend

```bash
cd backend
../mvnw.cmd clean package -DskipTests
java -jar target/lms-0.0.1-SNAPSHOT.jar
```

If you run commands from PowerShell in the project root, use:

```powershell
.\mvnw.cmd -version
cd .\backend
..\mvnw.cmd clean package -DskipTests
java -jar .\target\lms-0.0.1-SNAPSHOT.jar
```

Backend URL:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

The backend uses MySQL by default for local testing.

Default local MySQL connection:

```text
Database: edusphere_lms
Username: root
Password: root
```

Make sure your MySQL server is running before starting the backend. Local database settings live in `backend/.env`; update `DB_PASSWORD` there if your MySQL Workbench root password is not `root`.

### Storage Provider

Local storage is still the default. For a free beginner cloud setup, use Cloudinary.

`backend/.env` example:

```text
STORAGE_PROVIDER=cloudinary
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
CLOUDINARY_FOLDER=edusphere-lms
```

If `STORAGE_PROVIDER=local`, files stay in the local `storage` folder.

The backend URL includes `createDatabaseIfNotExist=true`, so the `edusphere_lms` database is created automatically when the MySQL username and password are correct. If your MySQL user does not have permission to create databases automatically, open `backend/create-database.sql` in MySQL Workbench and run it once before starting the backend.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend URL:

```text
http://localhost:5173
```

## MySQL Environment Variables

For deployment, set these variables in Render:

```text
DB_URL=jdbc:mysql://localhost:3306/edusphere_lms?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=root
DB_DRIVER=com.mysql.cj.jdbc.Driver
JWT_SECRET=replace-with-a-long-random-secret-key
FRONTEND_URL=https://your-vercel-app.vercel.app
```

For Vercel, set:

```text
VITE_API_BASE_URL=https://your-render-backend.onrender.com/api
```

## Deploy Backend on Render

1. Push this project to GitHub.
2. Create a new Render Web Service.
3. Select the GitHub repository.
4. Use Docker runtime.
5. Set root directory to `backend`.
6. Add the environment variables listed above.
7. Deploy.

Health check path:

```text
/actuator/health
```

## Deploy Frontend on Vercel

1. Import the same GitHub repository in Vercel.
2. Set root directory to `frontend`.
3. Add `VITE_API_BASE_URL`.
4. Deploy.

## Main API Endpoints

### Auth

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
```

### Public Courses

```text
GET /api/courses
GET /api/courses/{id}
```

### Student

```text
POST  /api/student/courses/{courseId}/enroll
GET   /api/student/enrollments
PATCH /api/student/lessons/{lessonId}/complete
GET   /api/student/courses/{courseId}/quizzes
POST  /api/student/quizzes/{quizId}/submit
```

### Instructor

```text
GET  /api/instructor/courses
POST /api/instructor/courses
PUT  /api/instructor/courses/{id}
POST /api/instructor/courses/{courseId}/modules
POST /api/instructor/modules/{moduleId}/lessons
POST /api/instructor/courses/{courseId}/quizzes
```

### Admin

```text
GET   /api/admin/stats
GET   /api/admin/users
GET   /api/admin/courses
PATCH /api/admin/courses/{courseId}/approve
```

## Resume Bullet Points

- Developed a full-stack Learning Management System using Spring Boot, Hibernate/JPA, MySQL, React, Bootstrap, and JWT-based Spring Security.
- Implemented role-based access control for Admin, Instructor, and Student workflows with protected REST APIs and frontend route guards.
- Built course enrollment, lesson progress tracking, quiz submission APIs, and admin course approval modules.
- Designed normalized JPA entity relationships for users, courses, modules, lessons, enrollments, quizzes, and progress records in MySQL.
- Deployed the React frontend on Vercel, Spring Boot backend on Render, and PostgreSQL database on a free managed cloud database service.

## Suggested GitHub Description

Full Stack LMS built with Spring Boot, Spring Security, Hibernate/JPA, MySQL, React, Bootstrap, JWT Auth, role-based dashboards, course enrollment, progress tracking, quizzes, and free-tier deployment.

## Future Improvements

- Certificate PDF generation
- Cloudinary video upload
- Payment integration with Razorpay test mode
- Instructor lesson editor UI
- Student quiz attempt UI
- Email notifications
- GitHub Actions CI/CD

## Production Architecture Path

Files added for the step-by-step production direction:

- [architecture/microservices-blueprint.md](C:/Users/admin/Documents/Codex/2026-05-15/can-you-give-one-full-stack/architecture/microservices-blueprint.md)
- [ops/docker-compose.microservices.yml](C:/Users/admin/Documents/Codex/2026-05-15/can-you-give-one-full-stack/ops/docker-compose.microservices.yml)
- [microservices/api-gateway/application.yml](C:/Users/admin/Documents/Codex/2026-05-15/can-you-give-one-full-stack/microservices/api-gateway/application.yml)
- [microservices/auth-service/application.yml](C:/Users/admin/Documents/Codex/2026-05-15/can-you-give-one-full-stack/microservices/auth-service/application.yml)
- [microservices/course-service/application.yml](C:/Users/admin/Documents/Codex/2026-05-15/can-you-give-one-full-stack/microservices/course-service/application.yml)
- [microservices/payment-service/application.yml](C:/Users/admin/Documents/Codex/2026-05-15/can-you-give-one-full-stack/microservices/payment-service/application.yml)

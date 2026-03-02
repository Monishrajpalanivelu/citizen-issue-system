# 🏙 Citizen Issue Processing System

A scalable backend platform for managing city infrastructure complaints such as potholes, water leaks, broken street lights, and sanitation issues.

Built for the **Smart City & Infrastructure Hackathon**, this system centralizes citizen issue reporting and provides geospatial analytics for city administrators.

---

## 🚨 Problem Statement

Urban maintenance is often inefficient due to:

* Lack of centralized complaint tracking
* Citizens unsure where/how to report issues
* Department silos causing duplication
* No visibility into city-wide infrastructure hotspots
* Delayed resolution due to poor routing

---

## 💡 Our Solution

A centralized backend system that:

* Enables citizens to report issues with GPS coordinates
* Automatically categorizes and routes complaints
* Tracks full issue lifecycle
* Provides geospatial heatmaps and cluster analytics
* Offers secure role-based access for different stakeholders

---

## 🔥 Key Features

### 🔐 Role-Based Security

* JWT Authentication
* Separate roles: Citizen / Staff / Admin
* Protected endpoints using Spring Security

### 🧠 Smart Routing

* Issues auto-tagged by category:

  * WATER_LEAK
  * POTHOLE
  * STREET_LIGHT
  * SANITATION
* Automatically assigned to relevant department

### 🌍 Geospatial Intelligence

* Store latitude & longitude per issue
* Nearby Issues API to avoid duplicate reports
* Geo-Cluster API for dashboard visualization
* District-level heatmap data

### 🔄 Status Tracking

Complete audit lifecycle:

```
OPEN → IN_PROGRESS → RESOLVED → CLOSED
```

Full status history maintained.

### 📊 Rich Analytics

* District-wise issue counts
* Category distribution stats
* Heatmap classification endpoints
* Dashboard statistics API

---

## 🛠 Tech Stack

| Layer              | Technology                     |
| ------------------ | ------------------------------ |
| Backend Framework  | Java Spring Boot 3             |
| Security           | Spring Security + JWT          |
| Database           | PostgreSQL                     |
| Database Migration | Flyway                         |
| API Documentation  | Swagger UI (Springdoc OpenAPI) |
| Build Tool         | Apache Maven                   |

---

## 🏗 Architecture Overview

The system follows a clean layered architecture:

```
Controller Layer
↓
Service Layer
↓
Repository Layer
↓
PostgreSQL Database
```

Separation of concerns:

* Controllers handle REST endpoints
* Services contain business logic
* Repositories manage data access
* DTOs isolate API contracts

---

## 🗄 Database Design (Core Entities)

### User

* id
* name
* email
* role
* password

### Issue

* id
* title
* description
* category
* priority
* latitude
* longitude
* status
* district
* created_at
* updated_at

### IssueStatusHistory

* id
* issue_id
* old_status
* new_status
* changed_by
* timestamp

---

## 📡 Important API Endpoints

### 🔹 Authentication

```
POST /api/v1/auth/register
POST /api/v1/auth/login
```

### 🔹 Issue Management

```
POST   /api/v1/issues
GET    /api/v1/issues/{id}
PUT    /api/v1/issues/{id}/status
PUT    /api/v1/issues/{id}/assign
GET    /api/v1/issues
```

### 🔹 Classification & Analytics

```
GET /api/v1/classify/heatmap
GET /api/v1/classify/nearby
GET /api/v1/dashboard/stats
```

---

## 🖥 System Demonstration (Screenshots Included)

The following screenshots are included in the project documentation/presentation:

1. HTML Dashboard

   ```
   http://localhost:8080/
   ```

2. Swagger UI Documentation

   ```
   http://localhost:8080/swagger-ui.html
   ```

3. Sample Heatmap JSON Response

   ```
   GET /api/v1/classify/heatmap
   ```

Example JSON response:

```json
{
  "district": "Central",
  "issueCount": 42,
  "categories": [
    {"category": "POTHOLE", "count": 18},
    {"category": "WATER_LEAK", "count": 10}
  ]
}
```

---

## 🚀 How To Run Locally

### 1️⃣ Clone Repository

```bash
git clone https://github.com/Monishrajpalanivelu/citizen-issue-system.git
cd citizen-issue-system
```

### 2️⃣ Configure Database

Create PostgreSQL database:

```sql
CREATE DATABASE smartcity;
```

Update `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/smartcity
    username: postgres
    password: your_password
```

### 3️⃣ Run Application

```bash
mvn clean install
mvn spring-boot:run
```

Application runs at:

```
http://localhost:8080
```

---

## 🔮 Future Enhancements

* 📱 Mobile App Integration
* 📸 Photo Upload & AI-based classification
* 📧 Email/SMS notifications
* 📊 Predictive analytics for infrastructure risk
* ☁ Cloud deployment with Docker & Kubernetes

---

## 🧪 Scalability Considerations

* Designed with stateless JWT authentication
* Layered architecture for easy microservice migration
* Database indexing for geo queries
* Flyway versioned migrations
* Ready for containerization (Docker)

---

## 📎 Repository

👉 [https://github.com/Monishrajpalanivelu/citizen-issue-system](https://github.com/Monishrajpalanivelu/citizen-issue-system)

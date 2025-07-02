# ZenLife API

![Java](https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=java&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=maven&logoColor=white)
![Spring](https://img.shields.io/badge/Spring-6DB33F?style=flat-square&logo=spring&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)

## Project Description

ZenLife API is a RESTful web service designed to facilitate various lifestyle management functionalities, including budgeting, daily planning, and communication features. This project aims to provide users with a seamless experience in managing their daily tasks, finances, and social interactions through a robust backend architecture.

### Key Features
- User authentication and authorization
- Daily planner for task management
- Budget tracking and expense management
- Real-time chat functionality
- Notification system for user interactions

## Tech Stack

| Technology | Description |
|------------|-------------|
| ![Java](https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=java&logoColor=white) Java | The primary programming language used for backend development. |
| ![Spring](https://img.shields.io/badge/Spring-6DB33F?style=flat-square&logo=spring&logoColor=white) Spring Framework | Framework for building the RESTful API. |
| ![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=maven&logoColor=white) Maven | Dependency management and build automation tool. |
| ![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white) Docker | Containerization platform for deploying the application. |

## Installation Instructions

### Prerequisites
- Java 11 or higher
- Maven 3.6 or higher
- Docker (optional, for containerization)

### Step-by-Step Installation
1. **Clone the repository:**
   ```bash
   git clone https://github.com/ThommyShelby9/zenlife_api.git
   cd zenlife_api
   ```

2. **Build the project using Maven:**
   ```bash
   mvn clean install
   ```

3. **Set up environment variables:**
   - Create a `.env` file in the root directory and add your configuration settings (if applicable).

4. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

## Usage

To interact with the API, you can use tools like Postman or cURL. Here are some basic usage examples:

- **User Registration:**
  ```http
  POST /api/auth/register
  ```

- **User Login:**
  ```http
  POST /api/auth/login
  ```

- **Add a Daily Task:**
  ```http
  POST /api/daily-planner/tasks
  ```

### Configuration
Configuration files are located in the `src/main/resources` directory. You can modify `application.properties`, `application-dev.properties`, and `application-prod.properties` to suit your environment settings.

## Project Structure

The project structure is organized as follows:

```
zenlife_api/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── api/
│   │   │           ├── expo/
│   │   │           │   ├── controllers/      # Contains REST controllers for handling requests
│   │   │           │   ├── models/           # Contains data models for the application
│   │   │           │   ├── repository/       # Contains interfaces for data access
│   │   │           │   └── services/         # Contains business logic
│   │   └── resources/
│   │       ├── application.properties         # Main configuration file
│   │       ├── application-dev.properties     # Development configuration
│   │       ├── application-prod.properties    # Production configuration
│   │       └── skills.json                    # JSON file for skills data
├── target/                                    # Compiled classes and resources
├── Dockerfile                                 # Docker configuration for containerization
└── pom.xml                                   # Maven configuration file
```

## Contributing

We welcome contributions! If you would like to contribute to this project, please follow these steps:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature/YourFeature`).
3. Make your changes and commit them (`git commit -m 'Add some feature'`).
4. Push to the branch (`git push origin feature/YourFeature`).
5. Open a pull request.

We appreciate your interest in improving the ZenLife API!

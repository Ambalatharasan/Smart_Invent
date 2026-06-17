# Smart Invent

**Smart Invent** is a web-based inventory management system and SaaS-style admin dashboard designed for small businesses to manage inventory items, restock planning, managers, activity logs, and stock analytics.

The application provides secure authentication, email verification, inventory tracking, restock order management, dashboard insights, manager controls, and deployment-ready configuration using Docker and Railway.

---

## Project Type

Web-based Inventory Management System

---

## Tech Stack

### Frontend
- HTML
- CSS
- JavaScript
- Tailwind CSS
- Flowbite-style UI
- ApexCharts

### Backend
- Java 21
- Spring Boot 3.3.5
- Spring Web MVC
- Spring Data JPA
- Hibernate

### Database
- MySQL 8
- H2 Database for local/development fallback

### Security
- Spring Security
- JWT Authentication
- BCrypt Password Hashing
- Email Verification OTP
- Strong Password Validation
- Verified-user-only login access
- Environment-variable based secrets

### Tools
- Maven
- VS Code / Eclipse
- PowerShell Scripts
- MySQL Workbench
- Postman / API Examples

### Deployment
- Docker
- Railway

---

## Main Features

- Inventory item management with add, edit, delete, receive stock, search, and filters
- Restock planning with suggested orders and received order tracking
- Manager list with fixed manager detail panel and assigned inventory
- Dashboard with KPI cards, stock analytics, low-stock watchlist, activity logs, and charts
- Secure login and registration flow with email verification OTP
- JWT-based authentication and BCrypt password hashing
- Strong password validation during registration
- Forgot password request flow
- Responsive SaaS-style admin dashboard
- Light/Dark mode support
- MySQL database integration
- Docker and Railway deployment support

---

## Special Features

- Auto-generated unique **Item.Num**
- Read-only **Item.Num** to prevent manual duplication
- Category dropdown with **Other** option
- INR currency formatting across the application
- Dashboard charts using ApexCharts
- Responsive sidebar and dashboard layout
- Activity logs for important inventory and restock actions
- Environment-variable based configuration for safer deployment

---

## Database Modules

The application manages the following modules:

- Users
- User Credentials
- Email Verification Codes
- Inventory Items
- Restock Orders
- Managers
- Activity Logs
- Dashboard Analytics Data

---

## Project Structure

```text
smart-inventory-manager-for-small-businesses/
├── Dockerfile
├── railway.json
├── .dockerignore
├── .gitignore
├── README.md
└── smart-invent-spring-backend/
    ├── pom.xml
    ├── README.md
    ├── .env.example
    ├── docker-compose.yml
    ├── API_EXAMPLES.http
    ├── src/
    │   ├── main/
    │   │   ├── java/com/stockwise/api/
    │   │   │   ├── config/
    │   │   │   ├── controller/
    │   │   │   ├── dto/
    │   │   │   ├── entity/
    │   │   │   ├── repository/
    │   │   │   ├── security/
    │   │   │   ├── service/
    │   │   │   └── StockwiseApiApplication.java
    │   │   └── resources/
    │   │       ├── application.properties
    │   │       ├── application-dev.properties
    │   │       ├── application-mysql.properties
    │   │       ├── application-railway.properties
    │   │       ├── db/mysql/
    │   │       └── static/
    │   └── test/
    │       └── java/com/stockwise/api/
```

---

## Application Modules

### Authentication Module
- User registration
- Login
- JWT token-based authentication
- BCrypt password hashing
- Email verification OTP
- Strong password validation
- Forgot password request
- Verified users only allowed to log in

### Inventory Module
- Add inventory item
- Edit inventory item
- Delete inventory item
- Receive stock
- Search inventory
- Filter inventory
- Auto-generated read-only Item.Num
- Category dropdown with custom category support

### Restock Module
- Suggested restock orders
- Restock planning
- Received order tracking
- Inventory quantity update after receiving stock
- Restock activity logging

### Manager Module
- Manager list
- Fixed manager detail panel
- Assigned inventory tracking
- Manager-based inventory control

### Dashboard Module
- KPI cards
- Stock analytics
- Inventory charts
- Low-stock watchlist
- Recent activity logs
- Manager control insights

---

## Security Features

- Secure login and registration
- JWT-based stateless authentication
- BCrypt password hashing
- Email verification OTP
- Strong password rules
- Forgot password request to admin
- Environment-variable based secret handling
- Verified-user-only access
- Protected dashboard routes

---

## Currency Support

All currency values in the application are formatted in **INR (₹)**.

Examples:
- Unit Cost
- Retail Price
- Inventory Value
- Restock Budget
- Estimated Cost
- Dashboard KPI values
- Chart tooltips

Money values are stored as numeric values in the database and formatted in the UI.

---

## Author

**Ambalatharasan RM**

- LinkedIn: https://www.linkedin.com/in/ambalatharasan-rm
- GitHub: https://github.com/Ambalatharasan

---

## License

This project is created for learning, portfolio, and demonstration purposes.

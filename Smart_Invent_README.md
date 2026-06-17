# Smart Invent

**Smart Invent** is a web-based inventory management system and SaaS-style admin dashboard designed for small businesses to manage inventory items, restock planning, managers, activity logs, and stock analytics.

The application provides secure authentication, email verification, inventory tracking, restock order management, dashboard insights, manager controls, and deployment-ready configuration using Docker and Railway.

---

## Project Type

Web-based Inventory Management System / SaaS-style Admin Dashboard

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

## Environment Variables

Create a `.env` file or configure environment variables in Railway.

```env
# Application
SERVER_PORT=8080
JWT_SECRET=your_jwt_secret_key

# Database
DB_URL=jdbc:mysql://localhost:3306/smart_inventory
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

# Mail
MAIL_ENABLED=true
MAIL_FROM=your_email@gmail.com
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your_email@gmail.com
SPRING_MAIL_PASSWORD=your_gmail_app_password

# Admin
STOCKWISE_ADMIN_EMAIL=admin_email@gmail.com

# Seed Users
STOCKWISE_SEED_USERS_ENABLED=false
STOCKWISE_SEED_ADMIN_NAME=Stockwise Admin
STOCKWISE_SEED_ADMIN_EMAIL=
```

> Do not commit `.env` files to GitHub.

---

## Local Setup

### 1. Clone the repository

```bash
git clone https://github.com/your-username/smart-inventory-manager-for-small-businesses.git
cd smart-inventory-manager-for-small-businesses/smart-invent-spring-backend
```

### 2. Configure database

Create a MySQL database:

```sql
CREATE DATABASE smart_inventory;
```

Update your environment variables or application profile with your MySQL credentials.

### 3. Build the project

```bash
mvn clean install
```

### 4. Run the application

```bash
mvn spring-boot:run
```

### 5. Open in browser

```text
http://localhost:8080
```

---

## Docker Setup

Build the Docker image:

```bash
docker build -t smart-invent .
```

Run the container:

```bash
docker run -p 8080:8080 smart-invent
```

Or use Docker Compose if configured:

```bash
docker-compose up --build
```

---

## Railway Deployment

This project includes Railway deployment support using:

- `Dockerfile`
- `railway.json`
- `application-railway.properties`
- Environment-variable based configuration

### Railway Steps

1. Push the project to GitHub
2. Create a new Railway project
3. Connect the GitHub repository
4. Add a MySQL database service
5. Add required environment variables
6. Deploy the application
7. Generate a public domain from Railway

---

## API Testing

API examples are available in:

```text
API_EXAMPLES.http
```

You can also test APIs using Postman.

---

## Suggested Test Flow

### Authentication
- Register a new user
- Receive email verification OTP
- Verify account
- Login with verified user
- Test forgot password request

### Inventory
- Add new item
- Verify Item.Num auto-generation
- Edit item
- Receive stock
- Search and filter items
- Delete item

### Restock
- Create suggested restock order
- Mark order as received
- Confirm stock quantity update
- Confirm activity log entry

### Dashboard
- Verify KPI cards
- Verify stock analytics charts
- Verify low-stock watchlist
- Verify recent activity logs

### Manager Module
- View manager list
- Select manager
- Check assigned inventory
- Verify manager detail panel

---

## Screenshots

Add your project screenshots here:

```text
screenshots/login.png
screenshots/dashboard.png
screenshots/inventory.png
screenshots/restock.png
screenshots/managers.png
```

Example:

```markdown
![Dashboard](screenshots/dashboard.png)
```

---

## Future Enhancements

- Role-based access control for Admin and Staff users
- Advanced report export in PDF/Excel
- Supplier management module
- Purchase order approval workflow
- Notification center
- Audit history dashboard
- Cloud database backup support

---

## Author

**Ambalatharasan RM**

- LinkedIn: https://www.linkedin.com/in/ambalatharasan-rm
- GitHub: https://github.com/Ambalatharasan

---

## License

This project is created for learning, portfolio, and demonstration purposes.

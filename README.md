# 🛍️ Retail Management System

[![CI](https://github.com/yasinttunc/Retail_Management_System/actions/workflows/ci.yml/badge.svg)](https://github.com/yasinttunc/Retail_Management_System/actions/workflows/ci.yml)

A simple **retail management console** built with **Spring Boot**. It lets you manage stores, products, stock levels, customer orders, and product reviews — all from a clean web dashboard.

The interesting part: it uses **two databases at the same time**:

- **MySQL** — for structured data (stores, products, customers, inventory, orders)
- **MongoDB** — for flexible data (customer reviews)

The backend serves both a **REST API** and a **single-page web UI** (plain HTML/CSS/JavaScript, no framework), so you can run the whole thing from one command.

---

## ✨ Features

- **Dashboard** — quick overview: total products, number of stores, units in stock, and low-stock alerts for the active store.
- **Products** — browse, search, filter by category, add, and delete products.
- **Inventory** — see and set stock levels per store.
- **Stores** — list all stores and add new ones.
- **Place Order** — build a shopping cart for a store and check out. Stock is automatically reduced when an order is placed.
- **Reviews** — read customer reviews (with star ratings) for a product in a store.

---

## 🧰 Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 4.1 |
| SQL database | MySQL |
| NoSQL database | MongoDB |
| Data access | Spring Data JPA (MySQL) + Spring Data MongoDB |
| Frontend | Plain HTML, CSS, JavaScript (served from `/static`) |
| Build tool | Maven (with the included `mvnw` wrapper) |

---

## 📦 Requirements

- **Java 21** (JDK)
- **Docker** — for the MySQL and MongoDB containers (or your own local MySQL and MongoDB)
- **Maven** — optional, the project ships with the `mvnw` wrapper

---

## 🚀 Getting Started

### 1. Clone the project

```bash
git clone https://github.com/yasinttunc/Retail_Management_System.git
cd Retail_Management_System
```

### 2. Start the databases (seeded automatically)

```bash
docker compose up -d
```

This starts MySQL on `3306` and MongoDB on `27017`. On the first start MySQL runs
[`db/mysql_seed.sql`](db/mysql_seed.sql) (schema + stores, products, customers, inventory, orders)
and MongoDB runs [`db/mongo_seed.js`](db/mongo_seed.js) (reviews). `docker compose down -v`
deletes the data so the seed runs again next time.

### 3. Run the application

```bash
./mvnw spring-boot:run
```

Open **http://localhost:8080** to see the dashboard with products, stock, stores and reviews.

### Configuration

Connection settings come from environment variables; the defaults match `docker-compose.yml`.

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/retail_management_system?createDatabaseIfNotExist=true` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | `retail` |
| `MONGODB_URI` | `mongodb://localhost:27017/reviews` |

Using your own MySQL instead of Docker? Set the variables above and load the seed with
`MYSQL_PWD='your-password' ./db/seed_mysql.sh` (set `MYSQL_BIN` if `mysql` is not on your `PATH`)
and `mongosh "mongodb://localhost:27017/reviews" db/mongo_seed.js`.

---

## 🧾 How orders are processed

`POST /store/placeOrder` is handled by `OrderService.saveOrder`, which runs in a single
transaction:

1. Validate the request: at least one line, positive quantities, existing store. Repeated lines for
   the same product are combined.
2. For every product, check it exists, is stocked in that store and has enough units. **Nothing is
   written until every line passes.**
3. Find the customer by email or create them.
4. Save the order header with a total calculated from **catalogue prices** (client-supplied prices
   are ignored).
5. Decrement stock and save each order line.

If anything fails, the whole order is rolled back and the API returns **HTTP 400** with a
`message` explaining why (for example `Not enough stock for Kettle: requested 6, available 5.`),
which the web UI shows to the user.

---

## ✅ Testing

```bash
./mvnw test
```

- `OrderServiceTest` — Mockito unit tests for the order workflow: stock decrements, catalogue
  pricing, new vs. existing customers, insufficient stock, combined duplicate lines, products not
  stocked in the store, unknown stores and invalid quantities (and that rejected orders write nothing).
- `StoreControllerTest` — success returns 200; a rejected order returns 400 with the reason.
- `RetailManagementSystemApplicationTests` — the Spring context starts against MySQL and MongoDB.

GitHub Actions runs the full suite on every push and pull request, with MySQL and MongoDB as
service containers.

---

## 🗂️ Project Structure

```
src/main/java/com/project/ibm/retailmanagementsystem/
├── controller/     → REST endpoints (Product, Inventory, Store, Review)
├── service/        → Business logic (OrderService, ServiceClass)
├── repository/     → Database access (JPA for MySQL, MongoDB for reviews)
├── model/          → Data entities (Product, Store, Inventory, Order..., Review)
├── dto/            → Request objects (e.g. PlaceOrderRequestDTO)
└── GlobalExceptionHandler.java

src/main/resources/
├── application.properties   → Database & app configuration
└── static/                  → Web UI (index.html, css/, js/app.js)

db/
├── mysql_seed.sql   → Sample SQL data
├── seed_mysql.sh    → Helper script to load the SQL
└── mongo_seed.js    → Sample review data for MongoDB
```

---

## 🗃️ Data Model (in plain words)

**In MySQL:**

- **Store** — a physical location (name, address).
- **Product** — an item you sell (name, category, price, unique SKU).
- **Customer** — a buyer (name, email, phone).
- **Inventory** — how many units of a product a store has (`stockLevel`). Links a product to a store.
- **OrderDetails** — one order (which customer, which store, total price, date).
- **OrderItem** — one line inside an order (product, quantity, price).

**In MongoDB:**

- **Review** — a customer's feedback: `rating` (1–5) and a `comment`, tied to a `productId`, `storeId`, and `customerId`.

---

## 🔌 API Reference

All responses are JSON. Base URL: `http://localhost:8080`

### Products
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/products/all` | List all products |
| `GET` | `/products/product/{id}` | Get a product by id |
| `POST` | `/products` | Add a product |
| `PUT` | `/products` | Update a product |
| `DELETE` | `/products/{id}` | Delete a product (and its inventory) |
| `GET` | `/products/searchProduct/{name}` | Search products by name |

### Inventory
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/inventory/{storeId}` | List products with stock levels for a store |
| `POST` | `/inventory` | Add a stock entry |
| `PUT` | `/inventory` | Update a stock entry |
| `DELETE` | `/inventory/{id}` | Remove a product from inventory |
| `GET` | `/inventory/validate/{quantity}/{storeId}/{productId}` | Check if enough stock exists |

### Stores
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/store` | Add a store |
| `GET` | `/store/validate/{storeId}` | Check if a store exists |
| `POST` | `/store/placeOrder` | Place an order (reduces stock automatically) |

### Reviews
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/reviews/{storeId}/{productId}` | Get reviews for a product in a store |
| `GET` | `/reviews` | Get all reviews |

#### Example: place an order
`POST /store/placeOrder`
```json
{
  "name": "John Smith",
  "email": "john@example.com",
  "phone": "+1-555-000-0000",
  "storeId": 1,
  "totalPrice": 1698.00,
  "purchaseProduct": [
    { "productId": 3, "quantity": 1, "price": 999.00 },
    { "productId": 5, "quantity": 1, "price": 299.00 }
  ]
}
```

---

## 🩺 Troubleshooting

**The app opens but everything is empty (0 products, 0 stock).**
Your seed data probably went into a different database than the one the app reads. Make sure `db/mysql_seed.sql` uses `retail_management_system` (the same name as in `application.properties`), then re-run the seed.

**Products show but everything says "Out of stock".**
The inventory response must include a `stockLevel` for each product. Check the `/inventory/{storeId}` endpoint returns stock, not just product data.

**Reviews say "No reviews found" even though data exists.**
The frontend expects the response wrapped as `{ "reviews": [...] }` with a `comment` field on each item. Make sure the Review endpoint uses those exact names.

**Can't connect to the database on startup.**
Confirm MySQL and MongoDB are running (`docker compose ps`) and that `DB_USERNAME` / `DB_PASSWORD` match your database.

---

## 📝 Notes

- The web UI talks to the backend using the endpoints listed above — there is no business logic in the frontend, it only displays data.
- Since MySQL data is persistent, your changes stay after a restart. But **re-running the seed script wipes and rebuilds the tables**, so anything you added through the UI will be lost.

---

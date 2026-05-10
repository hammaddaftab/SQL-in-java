# Cafe Management System — The Daily Grind

A full-stack cafe management web application with a React frontend and Java/Javalin backend.

## Architecture

- **Backend**: Java 19, Javalin 6 web framework, running on port 5000
- **Database**: SQLite (`cafe.db`) via the custom `sql_in_java` ORM
- **Frontend**: React 19 + Vite + TypeScript in `frontend/`, built into `src/main/resources/public/`
- **Build**: Maven shade plugin produces `target/sql-in-java-1.0.0.jar`
- **Fonts**: Fraunces (display) + Outfit (sans) via Google Fonts
- **Brand**: "The Daily Grind" — espresso/cream/amber palette

## Project Structure

- `frontend/` — React + Vite + TypeScript app
  - `src/api.ts` — Typed API client (all `fetch` calls)
  - `src/App.tsx` — React Router setup
  - `src/pages/` — Page components (customer + admin)
  - `src/components/` — Shared layouts (CustomerLayout, AdminLayout)
  - `src/index.css` — Global styles and design tokens
- `src/main/java/app/` — Application layer (Main, Database, StudDB, models)
- `src/main/java/sql_in_java/` — Custom ORM and SQL DSL library
- `src/main/resources/public/` — Built React app (committed output)
- `pom.xml` — Maven build configuration

## Running

The "Start application" workflow builds frontend then backend:
```
cd frontend && npm run build && cd .. && mvn package -q -DskipTests && java -jar target/sql-in-java-1.0.0.jar
```

## Credentials

- **Admin login**: password `admin123` at `/admin/login`
- **Customer login**: No auth needed — first name + last name on first visit, customerID stored in localStorage

## Key Features

### Customer (no auth required)
- Register once with first + last name → get a customerID stored in localStorage
- Browse menu by category (coffee, food, other)
- Cart persisted in localStorage
- Checkout with table selection, payment method (cash/card/online)
- View order history and order detail

### Admin (`/admin/login`, password: `admin123`)
- Dashboard: orders today, pending orders, sales today, low stock, top products
- Manage orders: filter by status, mark complete, confirm online orders
- Manage products: add, edit, delete (name, price, category)
- Inventory: ingredients with low-stock highlights, suppliers, restock requests
- Sales report: daily summary and top-selling products

## API Routes

### Public
- `GET /api/products` — all products
- `GET /api/tables` — all cafe tables
- `POST /api/customers` — register customer (firstName, lastName) → customerID
- `GET /api/customers/:id` — get customer by ID
- `POST /api/orders` — place order (customerID in body, no cookie)
- `GET /api/orders?customerId=X` — order history
- `GET /api/orders/:id?customerId=X` — order detail

### Admin (requires `session_id` cookie)
- `POST /api/admin/login` — login with password
- `GET /api/admin/dashboard` — dashboard stats
- `GET /api/admin/orders` — all orders (filter by ?status=)
- `POST /api/admin/orders/:id/status` — update order status
- `POST /api/admin/orders/:id/confirm` — confirm online order
- `GET /api/admin/products` — list products
- `POST /api/admin/products` — add product
- `PUT /api/admin/products/:id` — update product
- `DELETE /api/admin/products/:id` — delete product
- `GET /api/admin/inventory` — ingredients + suppliers
- `POST /api/admin/restock-requests` — request restock
- `GET /api/admin/sales` — sales report

## Seed Data

Database is seeded automatically on first run with:
- 4 suppliers (BeanMasters, FreshDairy, SweetSyrup, PastryPro)
- 10 ingredients
- 25 products across coffee, food, and drinks categories
- 10 cafe tables in various locations

## User Preferences

- Uses SQLite (not MySQL) for local development on Replit
- Port 5000 for the web server

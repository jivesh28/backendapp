# 💰 Smart Expense Categorizer API

An AI-powered expense management REST API built with **Java Spring Boot 3**, featuring intelligent auto-categorization, anomaly detection, and asynchronous bulk upload via Kafka.

---

## ✨ Key Features

| Feature | Description |
|---|---|
| 🤖 **Auto-Categorization** | Keyword matching first, then Gemini AI fallback |
| 🧠 **LLM Integration** | Google Gemini API discovers new categories dynamically |
| 🚨 **Anomaly Detection** | Flags expenses > 3× the 3-month category average |
| 📊 **Monthly Summary** | Spending breakdown by category per month |
| 📁 **Bulk Excel Upload** | Async `.xlsx` processing via Kafka |
| 🔐 **JWT Auth** | Multi-user support with stateless JWT authentication |
| 📖 **Swagger UI** | Interactive API docs at `/swagger-ui.html` |

---

## 🏗️ Architecture

```
Client (REST)
    │
    ▼
Spring Boot API (Port 8080)
├── Auth → JWT Filter → Controller → Service
├── Categorization: Keyword Match → Gemini LLM → Uncategorized
├── Anomaly: Compare vs 3-month rolling avg per category
└── Excel Upload → Kafka Topic: expense-upload
                        │
                  Kafka Consumer
                        │
                  Categorize + Flag + Persist → MySQL
```

### Categorization Flow

```
Expense Description
        │
        ▼
  Keyword Match?  ─── YES ──▶ Assign Category ✅
        │
       NO
        ▼
  Gemini LLM API  ─── OK ───▶ Assign/Create Category ✅
        │
      FAIL
        ▼
  "Uncategorized" ────────▶ Manual Override Available ✅
```

---

## 🧰 Tech Stack

| Component | Technology |
|---|---|
| Framework | Spring Boot 3.2, Java 17 |
| Database | MySQL 8 (JPA/Hibernate) |
| Message Queue | Apache Kafka (KRaft, no Zookeeper) |
| Auth | Spring Security + JWT (JJWT) |
| AI/LLM | Google Gemini 1.5 Flash API |
| Excel Parsing | Apache POI |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven |
| Local Infra | Docker Compose |

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose

### 1. Clone the repo
```bash
git clone <repo-url>
cd expense-categorizer
```

### 2. Configure your Gemini API Key
Edit `src/main/resources/application.yml`:
```yaml
gemini:
  api:
    key: "YOUR_GEMINI_API_KEY_HERE"
```

Get your free key: [Google AI Studio](https://aistudio.google.com)

### 3. Start infrastructure (MySQL + Kafka)
```bash
docker-compose up -d
```

### 4. Run the application
```bash
mvn spring-boot:run
```

App starts at: `http://localhost:8080`

### 5. Open Swagger UI
Navigate to: **http://localhost:8080/swagger-ui.html**

---

## 📡 API Endpoints

### Auth
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login → returns JWT token |

### Expenses
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/expenses` | Add expense (auto-categorize + anomaly check) |
| GET | `/api/expenses` | List expenses (filter: category, date range) |
| GET | `/api/expenses/summary?month=2&year=2026` | Monthly breakdown by category |
| GET | `/api/expenses/anomalies` | Get flagged unusual expenses |
| PUT | `/api/expenses/{id}/category` | Manually override category |

### Excel Upload (Async)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/expenses/upload` | Upload `.xlsx` (Kafka async processing) |
| GET | `/api/expenses/batch/{batchId}` | Poll batch processing status |

### Categories
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/categories` | List all categories (SYSTEM + LLM-discovered) |

---

## 🔐 Authentication

All endpoints (except `/api/auth/**`) require a Bearer JWT token.

```bash
# 1. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@test.com","password":"secret123"}'

# 2. Use the token
curl http://localhost:8080/api/expenses \
  -H "Authorization: Bearer <your-token>"
```

In Swagger UI: Click **Authorize** → paste `Bearer <token>`

---

## 📋 Sample API Calls

### Add Expense (Auto-Categorize)
```bash
curl -X POST http://localhost:8080/api/expenses \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 450.00,
    "description": "Swiggy dinner order",
    "date": "2026-02-21"
  }'
```

Response:
```json
{
  "success": true,
  "message": "Expense added successfully",
  "data": {
    "id": 1,
    "amount": 450.00,
    "description": "Swiggy dinner order",
    "category": "Food",
    "isAnomaly": false,
    "date": "2026-02-21"
  }
}
```

### Upload Excel
```bash
curl -X POST http://localhost:8080/api/expenses/upload \
  -H "Authorization: Bearer <token>" \
  -F "file=@expenses.xlsx"
```

**Expected Excel format (.xlsx):**
| amount | description | date |
|---|---|---|
| 450 | Swiggy order | 2026-02-10 |
| 1200 | Amazon purchase | 2026-02-12 |
| 299 | Netflix subscription | 2026-02-15 |

---

## 🚨 Anomaly Detection

An expense is flagged as anomalous when:
```
expense.amount > 3 × avg(last 90 days, same category, same user)
```

Example: If your average Food expense is ₹300, and you add ₹1200 for "Biryani Party",
the system will flag it with:
> *"Amount ₹1200.00 is 3.0x the 3-month average ₹400.00 for category 'Food'"*

The threshold multiplier is configurable in `application.yml`:
```yaml
anomaly:
  multiplier: 3.0
```

---

## 🗂️ Default Categories

| Category | Sample Keywords |
|---|---|
| Food | swiggy, zomato, dominos, mcdonald, restaurant |
| Transport | uber, ola, rapido, irctc, metro |
| Shopping | amazon, flipkart, myntra, meesho |
| Entertainment | netflix, spotify, hotstar, bookmyshow |
| Utilities | electricity, rent, wifi, jio, airtel |
| Healthcare | apollo, pharmacy, clinic, hospital |
| Fuel | petrol, diesel, bpcl, hpcl |
| Groceries | dmart, bigbasket, zepto, instamart |

> **New categories discovered by Gemini LLM are automatically saved** and appear in `GET /api/categories` with `createdBy: "LLM"`.

---

## 🛑 Stopping

```bash
docker-compose down        # Stop containers
docker-compose down -v     # Stop + remove volumes (wipes MySQL data)
```

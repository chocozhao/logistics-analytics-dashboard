# AI-Driven Logistics Analytics Dashboard

A full-stack web application that enables logistics teams to explore freight data through:
1. Traditional analytics dashboard (KPIs + charts)
2. AI-powered natural language interface
3. Predictive demand forecasting

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                         Frontend (Vue 3)                     │
└─────────────────────────────┬───────────────────────────────┘
│ REST / WebSocket
┌─────────────────────────────▼───────────────────────────────┐
│                     Spring Boot Backend                      │
│  /api/dashboard/kpis  /api/dashboard/charts  /api/query     │
│  /api/forecast                                              │
└─────────────────────────────┬───────────────────────────────┘
│
┌─────────────────────────────▼───────────────────────────────┐
│                       Orchestration Layer                    │
│  - NLU Service (AI + tool selector)                         │
│  - Tool Registry (analytical tools)                         │
│  - Query Planner (structured representation)                │
└─────────────────────────────┬───────────────────────────────┘
│
┌─────────────────────────────▼───────────────────────────────┐
│                       Tool Execution Layer                   │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────────────┐ │
│  │ KPI Aggregator│ │ Time Series  │ │   Forecasting Engine│ │
│  │ (SQL Builder) │ │ Query Executor│ │  (Exponential      │ │
│  └──────────────┘ └──────────────┘ │   Smoothing/Regression)│
└─────────────────────────────┬───────────────────────────────┘
│
┌─────────────────────────────▼───────────────────────────────┐
│                    PostgreSQL (Read-only)                   │
└─────────────────────────────────────────────────────────────┘
```

## Tech Stack

**Frontend:**
- Vue 3 + Vite + Pinia
- Element Plus (UI components)
- ECharts (chart rendering)

**Backend:**
- Java 17 + Spring Boot 3
- PostgreSQL 15
- LangChain4j + OpenAI GPT-3.5-turbo (AI orchestration)
- Apache Commons Math (forecasting)

**Deployment:**
- Docker + Docker Compose (local)
- Nginx (frontend serving)
- Render.com (cloud deployment)

## Core Principles

1. **AI as orchestrator, not source of truth** – AI interprets questions, selects tools, formats results; actual computations are performed by deterministic functions
2. **Read-only data** – no modifications to underlying datasets
3. **Explainability** – every answer includes filters used, metrics, query plan, and raw data access
4. **Simple, correct, deployable** – prioritize clarity and correctness over feature completeness

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Java 17 (for local backend development)
- Node.js 18+ (for local frontend development)
- OpenAI API key (for natural language queries)

### Quick Start with Docker

1. Clone the repository
2. Create a `.env` file in the root directory:
   ```
   OPENAI_API_KEY=your_openai_api_key_here
   ```
3. Run the application:
   ```bash
   docker-compose up
   ```
4. Access the application:
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080
   - PostgreSQL: localhost:5432

### Local Development

#### Backend Setup

```bash
cd backend
./mvnw spring-boot:run
```

The backend will start on http://localhost:8080

#### Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

The frontend will start on http://localhost:5173

#### Database Setup

```bash
# Using Docker
docker-compose up db

# Or manually with PostgreSQL
psql -U postgres -f database/init.sql
psql -U reader -d logistics -f database/sample-data.sql
```

## Deployment to Render.com

This application can be deployed to Render.com for public access:

1. **Push to GitHub**: Fork or push this repository to GitHub
2. **Sign up for Render**: Create an account at [Render.com](https://render.com)
3. **Connect GitHub**: Connect your GitHub repository to Render
4. **Automatic deployment**: Render will automatically detect `render.yaml` and deploy:
   - Backend service: `logistics-backend`
   - Frontend service: `logistics-frontend`  
   - PostgreSQL database: `logistics-db`
5. **Configure environment variables** in Render dashboard:
   - `OPENAI_API_KEY`: Your OpenAI API key for natural language queries
6. **Access your deployed application**:
   - Frontend: `https://logistics-analytics-dashboard-frontend.onrender.com/`
   - Backend API: `https://logistics-analytics-dashboard-yuzn.onrender.com/api`

**Note**: The first deployment may take 10-15 minutes to build and deploy all services.

### Manual Database Setup (if needed)

If you need to manually set up the database:

1. Connect to the Render PostgreSQL database using `psql` or pgAdmin
2. Run the migration script:
   ```bash
   psql -h [host] -U [user] -d logistics -f scripts/migrate-to-render.sql
   ```

### Environment Variables for Production

Create a `.env.production` file with:

```bash
# Required for production
OPENAI_API_KEY=your_openai_api_key_here
SPRING_PROFILES_ACTIVE=prod
CORS_ALLOWED_ORIGINS=https://logistics-frontend.onrender.com
VITE_API_URL=https://logistics-backend.onrender.com/api
```

## API Endpoints

### Dashboard APIs

- `GET /api/dashboard/kpis` – Key performance indicators
- `GET /api/dashboard/order-volume` – Time series order data
- `GET /api/dashboard/delivery-performance` – On-time vs delayed breakdown
- `GET /api/dashboard/carrier-breakdown` – Carrier performance metrics

### Natural Language Query

- `POST /api/query` – Ask questions in natural language

### Forecasting

- `POST /api/forecast` – Generate demand predictions

## AI Method Explanation

The system uses a structured approach to natural language queries:

1. **User asks question** (e.g., "Which carrier has the highest delay rate?")
2. **AI interprets question** using deepseek-v3.2 with a system prompt
3. **AI selects appropriate tool** from predefined tool definitions
4. **Backend executes deterministic query** with validated parameters
5. **Results formatted with explanation** including filters, metrics, query plan, and raw data

**Key Safety Feature**: AI never generates SQL or fabricates data values. All calculations are performed by deterministic backend functions.

## Forecasting Methodology

The system uses **Simple Exponential Smoothing (SES)** for demand forecasting:

- **Method**: `y_hat(t+1) = alpha * y(t) + (1-alpha) * y_hat(t)`
- **Alpha optimization**: Minimizes MSE on the last 20% of historical data
- **Fallback**: Linear regression if SES fails
- **Safety stock**: Forecast value × 1.2

## Assumptions and Limitations

1. **Dataset**: Single `orders` table with flattened structure
2. **Natural Language Support**: Limited to logistics domain vocabulary
3. **Forecasting**: Based on order volume only (not SKU-level)
4. **Real-time**: No streaming or real-time updates
5. **Authentication**: No authentication in v1 (deployed as read-only public demo)

## Future Improvements

1. **Query history and caching** (Redis)
2. **Advanced forecasting** (Holt-Winters for seasonality)
3. **SKU-level predictions**
4. **Authentication** (JWT + user roles)
5. **Real-time updates** (WebSocket)
6. **Comprehensive test suite**

## Development Notes

This project follows the principles from Anthropic's "Effective Harnesses for Long-Running Agents":
- Two-agent architecture (initializer + coder)
- Structured state tracking with `features.json`
- Incremental development, one feature at a time
- Self-verification through testing
- Clean session state with git commits

## License

[Add appropriate license]

## Acknowledgments

- Project structure inspired by Anthropic's long-running agent harnesses
- AI orchestration pattern based on LangChain4j tool-calling capabilities
- Forecasting methods from standard time series analysis literature
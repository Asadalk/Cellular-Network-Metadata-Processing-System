# Cellular Network Metadata Processing System Backend (FastAPI)

## Overview

This backend provides a **deterministic cell tower lookup service** based on cellular identifiers received from Android devices.

Given a cell’s **MCC, MNC, TAC, and CID**, the backend performs an **exact match lookup** against a **self hosted** Indian cell tower dataset and returns an **estimated geographic location** with a fixed-radius approximation.

This backend is designed for **system correctness and explainability**, not production-grade accuracy.

---

## System Architecture

Android Device  
└─ Extracts cell metadata (MCC, MNC, TAC, CID)  
↓  
HTTP POST /locate  
↓  
FastAPI Backend  
↓  
PostgreSQL (Cell Tower Dataset)  
↓  
Exact Match Lookup  
↓  
Location Response (lat, lon, radius)


---

## Technology Stack

- **Backend Framework:** FastAPI
- **Database:** PostgreSQL
- **DB Driver:** psycopg2
- **API Protocol:** REST (JSON)
- **Deployment Mode:** Local network (LAN)

---

## Dataset

The backend uses a **pre-existing Indian cell tower dataset** (MCC 404 & 405).

- Dataset documentation is maintained **separately**
- Backend assumes dataset is already cleaned and imported
- Only required fields are used:
  - `mcc`, `mnc`, `tac`, `cid`, `lat`, `lon`

The dataset itself is **not modified** by the backend.

---

## Database Schema

```sql
CREATE TABLE cell_towers (
    mcc INTEGER,
    mnc INTEGER,
    tac INTEGER,
    cid BIGINT,
    lat DOUBLE PRECISION,
    lon DOUBLE PRECISION
);
```
## API Endpoint

### POST `/locate`

Performs an exact-match lookup for a given cell identity.

**Request Body**  
```json
{
  "mcc": 405,
  "mnc": 856,
  "tac": 12345,
  "cid": 678901
}
```

**Success Response**  
```json
{
  "lat": 28.6139,
  "lon": 77.2090,
  "radius": 1000
}
```

**Failure Response**  
```json
{
  "error": "tower_not_found"
}
```

---

## Lookup Logic

- Performs exact match on (mcc, mnc, tac, cid)
- Returns the first matching tower
- If no match exists, returns an explicit error
- No fallback, approximation, or nearest-neighbor logic is used

This ensures deterministic and explainable behavior.

---

## Radius Model

- Radius is **hard coded** (1000 meters)
- Represents approximate coverage area
- No signal-strength or propagation modeling is applied

---

## Why Exact Match Only?

- Ensures deterministic behavior
- Avoids false positives
- Keeps system explainable

Nearest-neighbor or partial matching is intentionally excluded.

---

## Running the Backend

### Requirements

- Python 3.9+
- PostgreSQL
- FastAPI
- psycopg2
- Uvicorn

### Install Dependencies

```bash
pip install fastapi uvicorn psycopg2
```

### Start Server (LAN Accessible)

```bash
pip install fastapi uvicorn psycopg2
```

The backend will be accessible at:
```cpp
http://<YOUR_MACHINE_IP>:8000
```

---

## Android Integration Requirement

When integrating with Android:

- Replace `<YOUR_MACHINE_IP>` with the **actual local IP address** of the machine running the backend
- This IP must be updated in **both LTE and NR Retrofit base URLs**

Example:  
```kotlin
.baseUrl("http://192.168.1.10:8000/")
```

Backend **will not be reachable** if `localhost` or `127.0.0.1` is used on a physical device.

---

## API Testing (Without Android)

```bash
curl -X POST http://<YOUR_MACHINE_IP>:8000/locate \
-H "Content-Type: application/json" \
-d '{
  "mcc": 405,
  "mnc": 856,
  "tac": 12345,
  "cid": 678901
}'
```

---

## Limitations

- Dataset may not include newly deployed towers
- 5G NR cells may not be present
- Single-tower lookup only
- Fixed-radius estimation

These are **intentional MVP constraints**, not implementation gaps.

---

## Project Intent

This backend demonstrates:

- Clear data contracts
- Deterministic lookup logic
- Android–backend integration
- Ownership of backend decision-making

It prioritizes **clarity, correctness, and explainability** over scale or accuracy.

---

## License

Educational / demonstration use only.


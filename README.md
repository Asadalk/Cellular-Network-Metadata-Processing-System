# Cellular Network Metadata Processing System

## Overview

Cellular Network Metadata Processing System is an end-to-end system that extracts live cellular network metadata from an Android device and deterministically maps it to a geographic location using a backend lookup engine and a real Indian cell tower dataset.

The project focuses on **system design, data flow correctness, and explainability**, rather than production-grade location accuracy.

---

## System Architecture

Android Device  
→ TelephonyManager / CellInfo  
→ CellPayload (MCC, MNC, TAC, CID)  
→ JSON over HTTP (Retrofit)  
→ FastAPI Backend  
→ PostgreSQL Tower Dataset  
→ Deterministic Lookup Result  
→ Android Map Visualization (OpenStreetMap)  

---

## Core Components

### Android Client

* Extracts serving cell metadata using Android Telephony APIs
* Supports LTE and 5G NR inputs
* Handles Android version fragmentation
* Normalizes platform-specific data into a unified payload
* Sends metadata to backend via Retrofit
* Receives location response and visualizes it using OSMDroid

Required permissions:

* READ_PHONE_STATE
* ACCESS_FINE_LOCATION
* INTERNET

---

### Backend Service

* Built using FastAPI
* Exposes a single `/locate` endpoint
* Performs deterministic exact-match lookup on tower dataset
* Returns geographic coordinates with a fixed-radius estimate
* Explicitly reports lookup failures

---

### Database

* PostgreSQL database populated with Indian cell tower data (MCC 404 and 405)
* Dataset sourced from OpenCellID
* Minimal schema optimized for lookup correctness
* Uses exact-match queries without heuristics or estimation

---

## Location Visualization

* Implemented using **OSMDroid** with **OpenStreetMap** tiles
* Displays:

  * Marker at the resolved tower location
  * Fixed-radius coverage circle around the tower

The visualization is intentionally minimal and focuses on correctness rather than UI polish.

---

## Fixed Radius Estimation

The system returns a fixed coverage radius (1000 meters) for each resolved tower location.

This radius does not represent real-time signal propagation or precise user location.
It serves as a **conservative and explainable estimate** to visualize approximate tower coverage while avoiding misleading accuracy claims.

---

## How to Run the Project

### Prerequisites

* Android Studio
* Real Android device (required for telephony data access)
* Python 3.9+
* PostgreSQL
* Indian cell tower dataset (OpenCellID – MCC 404 & 405)

---

### Network Requirement (Important)

The Android device and the backend server **must be reachable over the same network**.

This can be achieved by:

* Connecting both devices to the **same Wi-Fi network**
* Using a **mobile hotspot**
* Running the backend on a **publicly accessible IP**

The backend base URL in the Android app must point to an IP address that is accessible from the device (localhost will not work).

---

### Backend Setup

1. Clone the repository
2. Create a PostgreSQL database
3. Create the `cell_towers` table
4. Import the tower dataset into the database
5. Install backend dependencies
6. Start the FastAPI server

The backend exposes a `/locate` endpoint that accepts cell identity metadata and returns tower location if found.

---

### Android App Setup

1. Open the Android project in Android Studio
2. Update backend base URL to point to the server IP
3. Grant required permissions on the device:

   * READ_PHONE_STATE
   * ACCESS_FINE_LOCATION
   * INTERNET
4. Run the app on a real Android device

The app automatically reads serving cell metadata, sends it to the backend, and visualizes the resolved tower location on the map.

---

## Design Decisions

* Deterministic exact-match lookup instead of heuristic estimation
* Static dataset to ensure full data ownership and explainability
* Fixed-radius visualization to avoid false precision
* Explicit handling of lookup failures
* No background services or continuous tracking

---

## Limitations

* Dataset may not include recently deployed towers
* No multi-tower triangulation
* Location accuracy is not production-grade

These limitations are intentional and documented.

---

## Tech Stack

* Android (Kotlin)
* FastAPI (Python)
* PostgreSQL
* Retrofit and Gson
* OSMDroid
* OpenStreetMap
* OpenCellID Dataset

---

## Status

MVP complete.  
System is fully functional, end-to-end integrated, and demonstrable.

---

## License

This project is intended for educational and research purposes.

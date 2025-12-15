from fastapi import FastAPI
import psycopg2
from dotenv import load_dotenv
import os

load_dotenv()

DB_HOST = os.getenv("DB_HOST")
DB_DATABASE = os.getenv("DB_DATABASE")
DB_USER = os.getenv("DB_USER")
DB_PASSWORD = os.getenv("DB_PASSWORD")

app = FastAPI()

conn = psycopg2.connect(
    host="DB_HOST",
    database="DB_DATABASE",
    user="DB_USER",
    password="DB_PASSWORD"
)

@app.post("/cell")
def receive_cell(data: dict):
    print(data)
    return {"status": "received"}

@app.post("/locate")
def locate_cell(data: dict):
    mcc = data.get("mcc")
    mnc = data.get("mnc")
    tac = data.get("tac")
    cid = data.get("cid")

    cur = conn.cursor()
    cur.execute(
        """
        SELECT lat, lon
        FROM cell_towers
        WHERE mcc=%s AND mnc=%s AND tac=%s AND cid=%s
        LIMIT 1;
        """,
        (mcc, mnc, tac, cid)
    )

    row = cur.fetchone()

    if row is None:
        return {"error": "tower_not_found"}

    lat, lon = row
    return {
        "lat": lat,
        "lon": lon,
        "radius": 1000
    }


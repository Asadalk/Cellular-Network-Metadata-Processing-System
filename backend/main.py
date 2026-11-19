from contextlib import contextmanager
from typing import Optional
import os

from dotenv import load_dotenv
from fastapi import FastAPI
from fastapi.responses import JSONResponse
from psycopg2.pool import ThreadedConnectionPool
from pydantic import BaseModel

load_dotenv()

DB_HOST = os.getenv("DB_HOST")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_DATABASE = os.getenv("DB_DATABASE")
DB_USER = os.getenv("DB_USER")
DB_PASSWORD = os.getenv("DB_PASSWORD")

app = FastAPI()

# A small pool instead of one shared connection: each request borrows a
# connection and returns it. Threaded pool because FastAPI runs plain `def`
# endpoints in a thread pool.
pool = ThreadedConnectionPool(
    minconn=1,
    maxconn=10,
    host=DB_HOST,
    port=DB_PORT,
    database=DB_DATABASE,
    user=DB_USER,
    password=DB_PASSWORD,
)


@contextmanager
def get_conn():
    conn = pool.getconn()
    try:
        yield conn
    finally:
        pool.putconn(conn)


class CellPayload(BaseModel):
    """Request body sent by the Android app. FastAPI rejects invalid input with 422."""
    mcc: int
    mnc: int
    tac: int
    cid: int
    rsrp: Optional[int] = None


@app.post("/cell")
def receive_cell(data: CellPayload):
    print(data)
    return {"status": "received"}


@app.post("/locate")
def locate_cell(data: CellPayload):
    with get_conn() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT lat, lon
                FROM cell_towers
                WHERE mcc=%s AND mnc=%s AND tac=%s AND cid=%s
                LIMIT 1;
                """,
                (data.mcc, data.mnc, data.tac, data.cid)
            )
            row = cur.fetchone()

    if row is None:
        return JSONResponse(status_code=404, content={"error": "tower_not_found"})

    lat, lon = row
    return {
        "lat": lat,
        "lon": lon,
        "radius": 1000
    }

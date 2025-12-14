from fastapi import FastAPI
import psycopg2

app = FastAPI()

conn = psycopg2.connect(
    host="localhost",
    database="tower_locator",
    user="postgres",
    password="masum"
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


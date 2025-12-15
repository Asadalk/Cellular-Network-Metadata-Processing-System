# Indian Cell Tower Dataset

This directory contains a **cleaned and reduced version of the cell tower dataset**
used by the backend.

The project itself is designed to work with the **full Indian mobile network
coverage dataset**, but only a **small cleaned subset** is committed to this
repository for practical reasons.

This is an intentional and standard engineering decision.

---

## Dataset Usage in This Project

- The backend logic is implemented and tested against the **full dataset**
  downloaded from Kaggle (derived from OpenCellID).
- The version committed to this repository is:
  - cleaned
  - schema-reduced
  - small in size
- The committed dataset exists to:
  - document the expected schema
  - allow quick local testing
  - keep the repository lightweight

The system behavior is **identical** when using the full dataset.

---

## Full Dataset Source

The complete dataset can be downloaded from Kaggle:

🔗 https://www.kaggle.com/datasets/sachinxshrivastav/mobile-network-coverage-india/data

The Kaggle dataset itself is derived from **OpenCellID**:

🔗 https://opencellid.org

---

## Attribution

- Original data source: **OpenCellID**
- Kaggle uploader: **sachinxshrivastav**
- Region: India
- Networks: GSM / LTE / others

This repository does **not redistribute** the full dataset.

---

## Dataset Schema (Used by Backend)

The backend requires only the following fields, which are present both in the
full dataset and in the committed subset:

| Column | Description |
|------|------------|
| mcc | Mobile Country Code |
| mnc | Mobile Network Code |
| tac | Tracking Area Code (or LAC) |
| cid | Cell ID |
| lat | Latitude (decimal degrees) |
| lon | Longitude (decimal degrees) |

All other columns from the original dataset are intentionally excluded, as they
are not required for tower location lookup.

---

## Using the Full Dataset (Recommended)

To run the project with realistic coverage:

1. Download the full dataset from Kaggle
2. Extract the required columns:
	mcc, mnc, tac (or lac), cid, lat, lon
3. Load the data into PostgreSQL using `\copy`
4. Run the backend normally

No code changes are required.

---

## Notes

- Tower coverage evolves over time
- Some towers may not be present in older datasets
- Location accuracy depends on data quality

These are inherent properties of telecom-based positioning systems.


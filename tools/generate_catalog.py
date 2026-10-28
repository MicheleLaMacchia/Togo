#!/usr/bin/env python3
"""
Genera il catalogo Togo applicando il mapping di migrazione v2.

Fonti:
  - tools/togo_catalog_migration_mapping_v2.csv
    (1.470 righe prodotto -> 748 prodotti canonici; le righe
    MERGE_ID_REFERENCE_REVIEW vengono fuse nel relativo survivor)

Output:
  - app/src/main/assets/catalog.db
      asset Room con tassonomia, prodotti canonici e sinonimi.
  - app/src/main/assets/catalog_migration_v2.sql
      script idempotente usato dalla migrazione Room 4->5 per rimappare
      i riferimenti utente e sostituire il catalogo sui database esistenti.
  - catalogo.md
      report leggibile con tutte le categorie L1/L2/L3 e tutti i prodotti.

Uso:
  python tools/generate_catalog.py
"""
from __future__ import annotations

import csv
import sqlite3
import sys
import unicodedata
from collections import OrderedDict
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MAPPING_PATH = ROOT / "tools" / "togo_catalog_migration_mapping_v2.csv"
ASSET_PATH = ROOT / "app" / "src" / "main" / "assets" / "catalog.db"
MIGRATION_SQL_PATH = ROOT / "app" / "src" / "main" / "assets" / "catalog_migration_v2.sql"
CATALOG_MD_PATH = ROOT / "catalogo.md"

EXPECTED_HEADERS = [
    "original_id",
    "original_name",
    "proposed_canonical_name",
    "proposed_survivor_id",
    "action",
    "old_l1",
    "old_l2",
    "old_l3",
    "category_conflict_in_group",
]
KEEP = "KEEP"
MERGE = "MERGE_ID_REFERENCE_REVIEW"
EXPECTED_L1_ORDER = [
    "Ortofrutta",
    "Panetteria",
    "Carne",
    "Pesce",
    "Bevande",
    "Banco Frigo",
    "Dispensa",
    "Non alimentare",
]
NON_FOOD_L1 = "Non alimentare"
EXPECTED_L1 = 8
EXPECTED_L2 = 37
EXPECTED_L3 = 98
EXPECTED_PRODUCTS = 748
EXPECTED_FOOD = 608
EXPECTED_NON_FOOD = 140

# Schema identico all'asset Room di partenza (user_version = 1).
# Le migrazioni Room 1->2, 2->3, 3->4 e 4->5 portano il DB alla versione 5.
DDL = [
    "CREATE TABLE TAXONOMY_LEVEL_1 (id INTEGER NOT NULL, name TEXT NOT NULL, sortOrder INTEGER NOT NULL, PRIMARY KEY(id))",
    "CREATE INDEX idx_taxonomy_l1_sort ON TAXONOMY_LEVEL_1(sortOrder)",
    "CREATE TABLE TAXONOMY_LEVEL_2 (id INTEGER NOT NULL, level1_id INTEGER NOT NULL, name TEXT NOT NULL, sortOrder INTEGER NOT NULL, PRIMARY KEY(id))",
    "CREATE INDEX idx_taxonomy_l2_l1 ON TAXONOMY_LEVEL_2(level1_id)",
    "CREATE INDEX idx_taxonomy_l2_sort ON TAXONOMY_LEVEL_2(sortOrder)",
    "CREATE TABLE TAXONOMY_LEVEL_3 (id INTEGER NOT NULL, level2_id INTEGER NOT NULL, name TEXT NOT NULL, sortOrder INTEGER NOT NULL, is_user_defined INTEGER NOT NULL, PRIMARY KEY(id))",
    "CREATE INDEX idx_taxonomy_l3_l2 ON TAXONOMY_LEVEL_3(level2_id)",
    "CREATE INDEX idx_taxonomy_l3_sort ON TAXONOMY_LEVEL_3(sortOrder)",
    "CREATE INDEX idx_taxonomy_l3_user_defined ON TAXONOMY_LEVEL_3(is_user_defined)",
    "CREATE TABLE CANONICAL_PRODUCT (id INTEGER NOT NULL, level3_id INTEGER NOT NULL, name TEXT NOT NULL, is_user_defined INTEGER NOT NULL, PRIMARY KEY(id))",
    "CREATE INDEX idx_canonical_product_l3 ON CANONICAL_PRODUCT(level3_id)",
    "CREATE INDEX idx_canonical_product_name ON CANONICAL_PRODUCT(name)",
    "CREATE INDEX idx_canonical_product_user_defined ON CANONICAL_PRODUCT(is_user_defined)",
    "CREATE TABLE SYNONYM (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, product_id INTEGER NOT NULL, term TEXT NOT NULL)",
    "CREATE INDEX idx_synonym_product_id ON SYNONYM(product_id)",
    "CREATE INDEX idx_synonym_term ON SYNONYM(term)",
    "CREATE TABLE SHOPPING_ITEM (id TEXT NOT NULL, productId INTEGER NOT NULL, quantity REAL NOT NULL, unit TEXT NOT NULL, brand TEXT, variant TEXT, condition TEXT, isChecked INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id))",
    "CREATE INDEX idx_shopping_item_product_id ON SHOPPING_ITEM(productId)",
    "CREATE INDEX idx_shopping_item_checked ON SHOPPING_ITEM(isChecked)",
    "CREATE TABLE HISTORICAL_ITEM (id TEXT NOT NULL, productId INTEGER NOT NULL, lastQuantity REAL NOT NULL, lastUnit TEXT NOT NULL, purchasedAt INTEGER NOT NULL, purchaseCount INTEGER NOT NULL, PRIMARY KEY(id))",
    "CREATE INDEX idx_historical_item_product_id ON HISTORICAL_ITEM(productId)",
    "CREATE INDEX idx_historical_item_purchased_at ON HISTORICAL_ITEM(purchasedAt)",
    "CREATE TABLE LEARNED_RULE (id TEXT NOT NULL, userExpression TEXT NOT NULL, productId INTEGER NOT NULL, level3Id INTEGER NOT NULL, lastAppliedAt INTEGER NOT NULL, isActive INTEGER NOT NULL, PRIMARY KEY(id))",
    "CREATE UNIQUE INDEX idx_learned_rule_expression ON LEARNED_RULE(userExpression)",
    "CREATE INDEX idx_learned_rule_product_id ON LEARNED_RULE(productId)",
    "CREATE INDEX idx_learned_rule_active ON LEARNED_RULE(isActive)",
]

EXPECTED_TABLES = {
    "TAXONOMY_LEVEL_1",
    "TAXONOMY_LEVEL_2",
    "TAXONOMY_LEVEL_3",
    "CANONICAL_PRODUCT",
    "SYNONYM",
    "SHOPPING_ITEM",
    "HISTORICAL_ITEM",
    "LEARNED_RULE",
}

EXPECTED_INDICES = {
    "idx_taxonomy_l1_sort",
    "idx_taxonomy_l2_l1",
    "idx_taxonomy_l2_sort",
    "idx_taxonomy_l3_l2",
    "idx_taxonomy_l3_sort",
    "idx_taxonomy_l3_user_defined",
    "idx_canonical_product_l3",
    "idx_canonical_product_name",
    "idx_canonical_product_user_defined",
    "idx_synonym_product_id",
    "idx_synonym_term",
    "idx_shopping_item_product_id",
    "idx_shopping_item_checked",
    "idx_historical_item_product_id",
    "idx_historical_item_purchased_at",
    "idx_learned_rule_expression",
    "idx_learned_rule_product_id",
    "idx_learned_rule_active",
}

MONTHS_IT = [
    "gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno",
    "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre",
]


def fail(message: str) -> None:
    raise SystemExit(f"ERRORE: {message}")


def parse_int(value: str, field: str, line_no: int) -> int:
    try:
        return int(value)
    except ValueError:
        fail(f"riga {line_no}: {field} non intero: {value!r}")


def normalize_term(text: str) -> str:
    """Minuscolo + accent-fold, la stessa normalizzazione usata da Kotlin."""
    decomposed = unicodedata.normalize("NFKD", text.strip().lower())
    return "".join(ch for ch in decomposed if not unicodedata.combining(ch))


def sql_quote(text: str) -> str:
    if "\n" in text or "\r" in text:
        fail(f"valore SQL con newline non supportato: {text!r}")
    return "'" + text.replace("'", "''") + "'"


def read_mapping() -> list[dict[str, object]]:
    if not MAPPING_PATH.exists():
        fail(f"mapping non trovato: {MAPPING_PATH}")

    with MAPPING_PATH.open(encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        if reader.fieldnames != EXPECTED_HEADERS:
            fail(f"intestazioni inattese: {reader.fieldnames}")

        rows: list[dict[str, object]] = []
        seen_ids: set[int] = set()
        for line_no, raw in enumerate(reader, start=2):
            row = {key: (raw.get(key) or "").strip() for key in EXPECTED_HEADERS}
            original_id = parse_int(str(row["original_id"]), "original_id", line_no)
            survivor_id = parse_int(
                str(row["proposed_survivor_id"]), "proposed_survivor_id", line_no
            )
            if original_id in seen_ids:
                fail(f"riga {line_no}: original_id duplicato {original_id}")
            seen_ids.add(original_id)

            action = str(row["action"])
            if action not in (KEEP, MERGE):
                fail(f"riga {line_no}: action inattesa {action!r}")
            if action == KEEP and survivor_id != original_id:
                fail(f"riga {line_no}: KEEP con survivor diverso da original_id")
            if action == KEEP and row["original_name"] != row["proposed_canonical_name"]:
                fail(f"riga {line_no}: KEEP con nome canonico diverso dal nome originale")
            if action == MERGE and survivor_id == original_id:
                fail(f"riga {line_no}: MERGE con survivor uguale a original_id")

            for field in ("original_name", "proposed_canonical_name", "old_l1", "old_l2", "old_l3"):
                if not row[field]:
                    fail(f"riga {line_no}: {field} vuoto")
            if row["category_conflict_in_group"] not in ("YES", "NO", ""):
                fail(
                    f"riga {line_no}: category_conflict_in_group inatteso "
                    f"{row['category_conflict_in_group']!r}"
                )

            row["original_id"] = original_id
            row["proposed_survivor_id"] = survivor_id
            rows.append(row)

    if not rows:
        fail("mapping vuoto")

    # Ogni survivor deve essere una riga KEEP e ogni MERGE deve puntare a un KEEP.
    survivors = {int(r["original_id"]): r for r in rows if r["action"] == KEEP}
    for row in rows:
        survivor_id = int(row["proposed_survivor_id"])
        survivor = survivors.get(survivor_id)
        if survivor is None:
            fail(f"survivor {survivor_id} non trovato per original_id {row['original_id']}")
        if row["proposed_canonical_name"] != survivor["proposed_canonical_name"]:
            fail(
                f"proposed_canonical_name incoerente per original_id {row['original_id']}: "
                f"{row['proposed_canonical_name']!r} != {survivor['proposed_canonical_name']!r}"
            )

    # I nomi canonici devono essere univoci anche dopo accent-fold.
    normalized_names: dict[str, int] = {}
    for survivor in survivors.values():
        name = str(survivor["proposed_canonical_name"])
        normalized = normalize_term(name)
        if normalized in normalized_names:
            fail(
                f"nome canonico duplicato dopo normalizzazione: {name!r} "
                f"(id {survivor['original_id']} e {normalized_names[normalized]})"
            )
        normalized_names[normalized] = int(survivor["original_id"])

    return rows


def build_taxonomy(rows: list[dict[str, object]]) -> dict[str, object]:
    l1_names: list[str] = []
    l2_keys: list[tuple[str, str]] = []
    l3_keys: list[tuple[str, str, str]] = []

    for row in rows:
        l1 = str(row["old_l1"])
        l2 = str(row["old_l2"])
        l3 = str(row["old_l3"])
        if l1 not in l1_names:
            l1_names.append(l1)
        if (l1, l2) not in l2_keys:
            l2_keys.append((l1, l2))
        if (l1, l2, l3) not in l3_keys:
            l3_keys.append((l1, l2, l3))

    if l1_names != EXPECTED_L1_ORDER:
        fail(f"ordine L1 inatteso: {l1_names}")
    if len(l1_names) != EXPECTED_L1:
        fail(f"L1 attesi {EXPECTED_L1}, trovati {len(l1_names)}")
    if len(l2_keys) != EXPECTED_L2:
        fail(f"L2 attesi {EXPECTED_L2}, trovati {len(l2_keys)}")
    if len(l3_keys) != EXPECTED_L3:
        fail(f"L3 attesi {EXPECTED_L3}, trovati {len(l3_keys)}")

    l1_ids = {name: index + 1 for index, name in enumerate(l1_names)}
    l2_ids = {key: index + 1 for index, key in enumerate(l2_keys)}
    l3_ids = {key: index + 1 for index, key in enumerate(l3_keys)}

    l2_sort: dict[tuple[str, str], int] = {}
    l2_counter: dict[str, int] = {}
    for l1, l2 in l2_keys:
        l2_counter[l1] = l2_counter.get(l1, 0) + 1
        l2_sort[(l1, l2)] = l2_counter[l1]

    l3_sort: dict[tuple[str, str, str], int] = {}
    l3_counter: dict[tuple[str, str], int] = {}
    for l1, l2, l3 in l3_keys:
        parent = (l1, l2)
        l3_counter[parent] = l3_counter.get(parent, 0) + 1
        l3_sort[(l1, l2, l3)] = l3_counter[parent]

    return {
        "l1_names": l1_names,
        "l1_ids": l1_ids,
        "l1_rows": [(l1_ids[name], name, index + 1) for index, name in enumerate(l1_names)],
        "l2_rows": [
            (l2_ids[(l1, l2)], l1_ids[l1], l2, l2_sort[(l1, l2)])
            for l1, l2 in l2_keys
        ],
        "l3_rows": [
            (l3_ids[(l1, l2, l3)], l2_ids[(l1, l2)], l3, l3_sort[(l1, l2, l3)])
            for l1, l2, l3 in l3_keys
        ],
        "l2_ids": l2_ids,
        "l3_ids": l3_ids,
    }


def build_products(
    rows: list[dict[str, object]], taxonomy: dict[str, object]
) -> list[dict[str, object]]:
    l3_ids: dict[tuple[str, str, str], int] = taxonomy["l3_ids"]  # type: ignore[assignment]
    survivors = sorted(
        (r for r in rows if r["action"] == KEEP),
        key=lambda r: int(r["proposed_survivor_id"]),
    )
    products: list[dict[str, object]] = []
    by_survivor: dict[int, dict[str, object]] = {}
    for row in survivors:
        path = (
            str(row["old_l1"]),
            str(row["old_l2"]),
            str(row["old_l3"]),
        )
        product = {
            "id": int(row["proposed_survivor_id"]),
            "name": str(row["proposed_canonical_name"]),
            "level3_id": l3_ids[path],
            "path": path,
            "aliases": [],
        }
        products.append(product)
        by_survivor[int(row["proposed_survivor_id"])] = product

    if len(products) != EXPECTED_PRODUCTS:
        fail(f"prodotti attesi {EXPECTED_PRODUCTS}, trovati {len(products)}")

    for row in sorted(rows, key=lambda r: int(r["original_id"])):
        if row["action"] == KEEP:
            continue
        survivor_id = int(row["proposed_survivor_id"])
        by_survivor[survivor_id]["aliases"].append(str(row["original_name"]))  # type: ignore[union-attr]

    return products


def build_synonyms(products: list[dict[str, object]]) -> dict[str, int]:
    """Termine normalizzato -> product_id.

    I nomi canonici completi vengono inseriti per primi, così nessun alias può
    sottrarre un nome canonico a un altro prodotto. Poi vengono aggiunte le
    basi (prime due parole) e infine i nomi originali delle righe fuse.
    """
    terms: dict[str, int] = {}

    def add(term: str, product_id: int) -> None:
        normalized = normalize_term(term)
        if normalized and normalized not in terms:
            terms[normalized] = product_id

    for product in products:
        add(str(product["name"]), int(product["id"]))

    for product in products:
        parts = str(product["name"]).split()
        if len(parts) > 2:
            add(" ".join(parts[:2]), int(product["id"]))

    for product in products:
        for alias in product["aliases"]:  # type: ignore[union-attr]
            add(str(alias), int(product["id"]))

    products_with_terms = {pid for pid in terms.values()}
    missing = {int(p["id"]) for p in products} - products_with_terms
    if missing:
        fail(f"prodotti senza sinonimo: {sorted(missing)}")
    return terms


def create_database(
    taxonomy: dict[str, object],
    products: list[dict[str, object]],
    synonyms: dict[str, int],
) -> None:
    ASSET_PATH.parent.mkdir(parents=True, exist_ok=True)
    if ASSET_PATH.exists():
        ASSET_PATH.unlink()

    conn = sqlite3.connect(str(ASSET_PATH))
    try:
        for statement in DDL:
            conn.execute(statement)

        conn.executemany(
            "INSERT INTO TAXONOMY_LEVEL_1 (id, name, sortOrder) VALUES (?,?,?)",
            taxonomy["l1_rows"],
        )
        conn.executemany(
            "INSERT INTO TAXONOMY_LEVEL_2 (id, level1_id, name, sortOrder) VALUES (?,?,?,?)",
            taxonomy["l2_rows"],
        )
        conn.executemany(
            "INSERT INTO TAXONOMY_LEVEL_3 (id, level2_id, name, sortOrder, is_user_defined) VALUES (?,?,?,?,0)",
            taxonomy["l3_rows"],
        )
        conn.executemany(
            "INSERT INTO CANONICAL_PRODUCT (id, level3_id, name, is_user_defined) VALUES (?,?,?,0)",
            [(int(p["id"]), int(p["level3_id"]), str(p["name"])) for p in products],
        )
        conn.executemany(
            "INSERT INTO SYNONYM (product_id, term) VALUES (?,?)",
            sorted(((pid, term) for term, pid in synonyms.items()), key=lambda kv: (kv[0], kv[1])),
        )
        conn.execute("PRAGMA user_version = 1")
        conn.commit()
        conn.execute("VACUUM")
    finally:
        conn.close()


def write_migration_sql(
    rows: list[dict[str, object]],
    taxonomy: dict[str, object],
    products: list[dict[str, object]],
    synonyms: dict[str, int],
) -> None:
    """Scrive lo script SQL eseguito dalla migrazione Room 4->5.

    Lo script è idempotente sul catalogo: elimina solo i prodotti predefiniti
    (is_user_defined = 0), rimappa i riferimenti utente tramite una tabella
    temporanea e reinserisce il nuovo catalogo. I prodotti creati dall'utente
    restano intatti.
    """
    lines: list[str] = []
    lines.append("DROP TABLE IF EXISTS CATALOG_REMAP_V2")
    lines.append(
        "CREATE TEMP TABLE CATALOG_REMAP_V2 "
        "(old_id INTEGER PRIMARY KEY, new_id INTEGER NOT NULL)"
    )

    for row in sorted(rows, key=lambda r: int(r["original_id"])):
        lines.append(
            "INSERT INTO CATALOG_REMAP_V2 (old_id, new_id) VALUES "
            f"({int(row['original_id'])}, {int(row['proposed_survivor_id'])})"
        )

    lines.extend(
        [
            "UPDATE SHOPPING_ITEM SET productId = "
            "(SELECT new_id FROM CATALOG_REMAP_V2 WHERE old_id = SHOPPING_ITEM.productId) "
            "WHERE productId IN (SELECT old_id FROM CATALOG_REMAP_V2) "
            "AND productId IN (SELECT id FROM CANONICAL_PRODUCT WHERE is_user_defined = 0)",
            "UPDATE HISTORICAL_ITEM SET productId = "
            "(SELECT new_id FROM CATALOG_REMAP_V2 WHERE old_id = HISTORICAL_ITEM.productId) "
            "WHERE productId IN (SELECT old_id FROM CATALOG_REMAP_V2) "
            "AND productId IN (SELECT id FROM CANONICAL_PRODUCT WHERE is_user_defined = 0)",
            "UPDATE LEARNED_RULE SET productId = "
            "(SELECT new_id FROM CATALOG_REMAP_V2 WHERE old_id = LEARNED_RULE.productId) "
            "WHERE productId IN (SELECT old_id FROM CATALOG_REMAP_V2) "
            "AND productId IN (SELECT id FROM CANONICAL_PRODUCT WHERE is_user_defined = 0)",
            "DELETE FROM SYNONYM WHERE product_id IN "
            "(SELECT id FROM CANONICAL_PRODUCT WHERE is_user_defined = 0)",
            "DELETE FROM CANONICAL_PRODUCT WHERE is_user_defined = 0",
        ]
    )

    # Ripristina la tassonomia anche su un DB dev che non aveva il catalogo.
    # INSERT OR IGNORE lascia intatte le righe già presenti e le categorie
    # create dall'utente (che hanno ID successivi a quelli predefiniti).
    for row in taxonomy["l1_rows"]:  # type: ignore[union-attr]
        taxonomy_id, name, sort_order = row
        lines.append(
            "INSERT OR IGNORE INTO TAXONOMY_LEVEL_1 (id, name, sortOrder) VALUES "
            f"({taxonomy_id}, {sql_quote(str(name))}, {sort_order})"
        )
    for row in taxonomy["l2_rows"]:  # type: ignore[union-attr]
        taxonomy_id, level1_id, name, sort_order = row
        lines.append(
            "INSERT OR IGNORE INTO TAXONOMY_LEVEL_2 (id, level1_id, name, sortOrder) VALUES "
            f"({taxonomy_id}, {level1_id}, {sql_quote(str(name))}, {sort_order})"
        )
    for row in taxonomy["l3_rows"]:  # type: ignore[union-attr]
        taxonomy_id, level2_id, name, sort_order = row
        lines.append(
            "INSERT OR IGNORE INTO TAXONOMY_LEVEL_3 "
            "(id, level2_id, name, sortOrder, is_user_defined) VALUES "
            f"({taxonomy_id}, {level2_id}, {sql_quote(str(name))}, {sort_order}, 0)"
        )

    for product in products:
        lines.append(
            "INSERT INTO CANONICAL_PRODUCT (id, level3_id, name, is_user_defined) VALUES "
            f"({int(product['id'])}, {int(product['level3_id'])}, "
            f"{sql_quote(str(product['name']))}, 0)"
        )

    for term, product_id in sorted(synonyms.items(), key=lambda kv: (kv[1], kv[0])):
        lines.append(
            "INSERT INTO SYNONYM (product_id, term) VALUES "
            f"({product_id}, {sql_quote(term)})"
        )

    lines.append("DROP TABLE CATALOG_REMAP_V2")
    for line in lines:
        if "\n" in line or "\r" in line:
            fail("statement SQL con newline inatteso")
    MIGRATION_SQL_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")


def format_int(value: int) -> str:
    return f"{value:,}".replace(",", ".")


def read_catalog(conn: sqlite3.Connection) -> list[dict[str, object]]:
    conn.row_factory = sqlite3.Row
    rows = conn.execute(
        """
        SELECT t1.id AS l1_id, t1.name AS l1_name, t1.sortOrder AS l1_sort,
               t2.id AS l2_id, t2.name AS l2_name, t2.sortOrder AS l2_sort,
               t3.id AS l3_id, t3.name AS l3_name, t3.sortOrder AS l3_sort,
               cp.id AS product_id, cp.name AS product_name
        FROM TAXONOMY_LEVEL_1 t1
        JOIN TAXONOMY_LEVEL_2 t2 ON t2.level1_id = t1.id
        JOIN TAXONOMY_LEVEL_3 t3 ON t3.level2_id = t2.id
        LEFT JOIN CANONICAL_PRODUCT cp ON cp.level3_id = t3.id
        WHERE t3.is_user_defined = 0
        ORDER BY t1.sortOrder, t2.sortOrder, t3.sortOrder, cp.name COLLATE NOCASE, cp.id
        """
    ).fetchall()
    return [dict(row) for row in rows]


def write_catalog_md(source_rows: int, merged_rows: int) -> None:
    conn = sqlite3.connect(str(ASSET_PATH))
    try:
        rows = read_catalog(conn)
        total_products = conn.execute("SELECT COUNT(*) FROM CANONICAL_PRODUCT").fetchone()[0]
        n_l1 = conn.execute("SELECT COUNT(*) FROM TAXONOMY_LEVEL_1").fetchone()[0]
        n_l2 = conn.execute("SELECT COUNT(*) FROM TAXONOMY_LEVEL_2").fetchone()[0]
        n_l3 = conn.execute(
            "SELECT COUNT(*) FROM TAXONOMY_LEVEL_3 WHERE is_user_defined = 0"
        ).fetchone()[0]
    finally:
        conn.close()

    l1s: "OrderedDict[str, dict[str, object]]" = OrderedDict()
    for row in rows:
        l1_name = str(row["l1_name"])
        l2_name = str(row["l2_name"])
        l3_name = str(row["l3_name"])
        l1 = l1s.setdefault(l1_name, {"l2": OrderedDict(), "products": 0})
        l2 = l1["l2"].setdefault(l2_name, {"l3": OrderedDict(), "products": 0})  # type: ignore[index]
        l3 = l2["l3"].setdefault(l3_name, {"products": []})  # type: ignore[index]
        if row["product_id"] is not None:
            l3["products"].append((int(row["product_id"]), str(row["product_name"])))  # type: ignore[index]
            l2["products"] += 1  # type: ignore[index]
            l1["products"] += 1  # type: ignore[index]

    today = date.today()
    date_text = f"{today.day} {MONTHS_IT[today.month - 1]} {today.year}"

    lines: list[str] = [
        "# Catalogo prodotti Togo",
        "",
        "> Documento generato automaticamente dal database `app/src/main/assets/catalog.db`.",
        "> Fonte: `tools/togo_catalog_migration_mapping_v2.csv`.",
        f"> Data di generazione: **{date_text}**.",
        "",
        "## Riepilogo",
        "",
        "| Livello | Categorie |",
        "|---|---:|",
        f"| Livello 1 | {format_int(n_l1)} |",
        f"| Livello 2 | {format_int(n_l2)} |",
        f"| Livello 3 | {format_int(n_l3)} |",
        f"| **Totale categorie** | **{format_int(n_l1 + n_l2 + n_l3)}** |",
        f"| **Prodotti censiti** | **{format_int(total_products)}** |",
        "",
        f"> Il mapping di origine contiene {format_int(source_rows)} nomi prodotto; "
        f"{format_int(merged_rows)} confezioni/varianti sono state fuse nei "
        f"{format_int(total_products)} prodotti canonici e restano ricercabili come sinonimi.",
        "",
        "I prodotti sono elencati in ordine alfabetico all'interno di ogni categoria di livello 3. "
        "Gli ID corrispondono a `CANONICAL_PRODUCT.id`.",
        "",
        "## Indice generale",
        "",
    ]

    for index, (l1_name, l1) in enumerate(l1s.items(), start=1):
        n_l2_l1 = len(l1["l2"])  # type: ignore[arg-type]
        n_l3_l1 = sum(len(l2["l3"]) for l2 in l1["l2"].values())  # type: ignore[union-attr]
        lines.append(
            f"{index}. **{l1_name}** — {format_int(n_l2_l1)} categorie L2, "
            f"{format_int(n_l3_l1)} categorie L3, {format_int(int(l1['products']))} prodotti"
        )
    lines.extend(["", "---", ""])

    for l1_index, (l1_name, l1) in enumerate(l1s.items(), start=1):
        n_l2_l1 = len(l1["l2"])  # type: ignore[arg-type]
        n_l3_l1 = sum(len(l2["l3"]) for l2 in l1["l2"].values())  # type: ignore[union-attr]
        lines.extend(
            [
                f"## {l1_index}. {l1_name}",
                "",
                f"**{format_int(n_l2_l1)} categorie L2 — {format_int(n_l3_l1)} categorie L3 — "
                f"{format_int(int(l1['products']))} prodotti**",
                "",
            ]
        )
        for l2_index, (l2_name, l2) in enumerate(l1["l2"].items(), start=1):  # type: ignore[union-attr]
            n_l3_l2 = len(l2["l3"])  # type: ignore[index]
            lines.extend(
                [
                    f"### {l1_index}.{l2_index}. {l2_name}",
                    "",
                    f"**{format_int(n_l3_l2)} categorie L3 — {format_int(int(l2['products']))} prodotti**",
                    "",
                ]
            )
            for l3_index, (l3_name, l3) in enumerate(l2["l3"].items(), start=1):  # type: ignore[index]
                products = sorted(l3["products"], key=lambda item: (item[1].casefold(), item[0]))  # type: ignore[index]
                lines.extend(
                    [
                        f"#### {l1_index}.{l2_index}.{l3_index}. {l3_name} "
                        f"({format_int(len(products))} prodotti)",
                        "",
                        "| ID | Prodotto |",
                        "|---:|---|",
                    ]
                )
                for product_id, product_name in products:
                    safe_name = product_name.replace("|", "\\|")
                    lines.append(f"| {product_id} | {safe_name} |")
                lines.append("")
        lines.extend(["---", ""])
    CATALOG_MD_PATH.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")


def verify_database(
    taxonomy: dict[str, object],
    products: list[dict[str, object]],
    synonyms: dict[str, int],
) -> None:
    conn = sqlite3.connect(str(ASSET_PATH))
    try:
        conn.row_factory = sqlite3.Row
        tables = {
            row[0]
            for row in conn.execute("SELECT name FROM sqlite_master WHERE type='table'")
        }
        missing_tables = EXPECTED_TABLES - tables
        if missing_tables:
            fail(f"tabelle mancanti: {sorted(missing_tables)}")

        indices = {
            row[0]
            for row in conn.execute(
                "SELECT name FROM sqlite_master WHERE type='index' AND name NOT LIKE 'sqlite_%'"
            )
        }
        missing_indices = EXPECTED_INDICES - indices
        if missing_indices:
            fail(f"indici mancanti: {sorted(missing_indices)}")

        user_version = conn.execute("PRAGMA user_version").fetchone()[0]
        if user_version != 1:
            fail(f"user_version atteso 1, trovato {user_version}")

        counts = {
            "L1": conn.execute("SELECT COUNT(*) FROM TAXONOMY_LEVEL_1").fetchone()[0],
            "L2": conn.execute("SELECT COUNT(*) FROM TAXONOMY_LEVEL_2").fetchone()[0],
            "L3": conn.execute("SELECT COUNT(*) FROM TAXONOMY_LEVEL_3").fetchone()[0],
            "products": conn.execute("SELECT COUNT(*) FROM CANONICAL_PRODUCT").fetchone()[0],
            "synonyms": conn.execute("SELECT COUNT(*) FROM SYNONYM").fetchone()[0],
        }
        expected_counts = {
            "L1": EXPECTED_L1,
            "L2": EXPECTED_L2,
            "L3": EXPECTED_L3,
            "products": EXPECTED_PRODUCTS,
        }
        for key, expected in expected_counts.items():
            if counts[key] != expected:
                fail(f"{key} attesi {expected}, trovati {counts[key]}")

        food = conn.execute(
            """
            SELECT COUNT(*) FROM CANONICAL_PRODUCT cp
            JOIN TAXONOMY_LEVEL_3 t3 ON t3.id = cp.level3_id
            JOIN TAXONOMY_LEVEL_2 t2 ON t2.id = t3.level2_id
            JOIN TAXONOMY_LEVEL_1 t1 ON t1.id = t2.level1_id
            WHERE t1.name != ?
            """,
            (NON_FOOD_L1,),
        ).fetchone()[0]
        non_food = conn.execute(
            """
            SELECT COUNT(*) FROM CANONICAL_PRODUCT cp
            JOIN TAXONOMY_LEVEL_3 t3 ON t3.id = cp.level3_id
            JOIN TAXONOMY_LEVEL_2 t2 ON t2.id = t3.level2_id
            JOIN TAXONOMY_LEVEL_1 t1 ON t1.id = t2.level1_id
            WHERE t1.name = ?
            """,
            (NON_FOOD_L1,),
        ).fetchone()[0]
        if (food, non_food) != (EXPECTED_FOOD, EXPECTED_NON_FOOD):
            fail(
                f"alimentari/non alimentari attesi "
                f"{EXPECTED_FOOD}/{EXPECTED_NON_FOOD}, trovati {food}/{non_food}"
            )

        invalid = conn.execute(
            """
            SELECT COUNT(*) FROM CANONICAL_PRODUCT
            WHERE level3_id IS NULL OR TRIM(name) = '' OR is_user_defined != 0
            """
        ).fetchone()[0]
        if invalid:
            fail(f"prodotti seed invalidi: {invalid}")

        duplicates = conn.execute(
            "SELECT COUNT(*) FROM (SELECT name FROM CANONICAL_PRODUCT "
            "GROUP BY name HAVING COUNT(*) > 1)"
        ).fetchone()[0]
        if duplicates:
            fail(f"nomi prodotto duplicati: {duplicates}")

        orphan_synonyms = conn.execute(
            """
            SELECT COUNT(*) FROM SYNONYM s
            LEFT JOIN CANONICAL_PRODUCT p ON p.id = s.product_id
            WHERE p.id IS NULL
            """
        ).fetchone()[0]
        if orphan_synonyms:
            fail(f"sinonimi orfani: {orphan_synonyms}")

        duplicate_terms = conn.execute(
            "SELECT COUNT(*) FROM (SELECT term FROM SYNONYM "
            "GROUP BY term HAVING COUNT(*) > 1)"
        ).fetchone()[0]
        if duplicate_terms:
            fail(f"termini sinonimo duplicati: {duplicate_terms}")

        no_synonyms = conn.execute(
            """
            SELECT COUNT(*) FROM CANONICAL_PRODUCT p
            WHERE NOT EXISTS (SELECT 1 FROM SYNONYM s WHERE s.product_id = p.id)
            """
        ).fetchone()[0]
        if no_synonyms:
            fail(f"prodotti senza sinonimo: {no_synonyms}")

        empty_l3 = conn.execute(
            """
            SELECT COUNT(*) FROM TAXONOMY_LEVEL_3 t3
            WHERE t3.is_user_defined = 0
              AND NOT EXISTS (SELECT 1 FROM CANONICAL_PRODUCT cp WHERE cp.level3_id = t3.id)
            """
        ).fetchone()[0]
        if empty_l3:
            fail(f"categorie L3 senza prodotti: {empty_l3}")

        integrity = conn.execute("PRAGMA integrity_check").fetchone()[0]
        if integrity != "ok":
            fail(f"integrity_check: {integrity}")

        # Verifica puntuale del mapping: ID, nome e categoria di ogni survivor.
        l3_ids: dict[tuple[str, str, str], int] = taxonomy["l3_ids"]  # type: ignore[assignment]
        product_by_id = {
            int(p["id"]): p for p in products
        }
        for row in conn.execute(
            "SELECT id, level3_id, name FROM CANONICAL_PRODUCT ORDER BY id"
        ):
            product = product_by_id[int(row["id"])]
            if row["name"] != product["name"]:
                fail(f"nome prodotto {row['id']} diverso dal mapping")
            if int(row["level3_id"]) != l3_ids[product["path"]]:  # type: ignore[index]
                fail(f"categoria prodotto {row['id']} diversa dal mapping")

        print(f"DB: {ASSET_PATH} ({ASSET_PATH.stat().st_size / 1024:.1f} KB)")
        print(
            f"  tassonomia: {counts['L1']} L1, {counts['L2']} L2, {counts['L3']} L3"
        )
        print(f"  prodotti: {counts['products']} ({food} alimentari, {non_food} non alimentari)")
        print(f"  sinonimi: {counts['synonyms']}")
        print("  schema/indici: OK")
        print(f"  user_version: {user_version}")
    finally:
        conn.close()


def main() -> int:
    rows = read_mapping()
    taxonomy = build_taxonomy(rows)
    products = build_products(rows, taxonomy)
    synonyms = build_synonyms(products)

    create_database(taxonomy, products, synonyms)
    write_migration_sql(rows, taxonomy, products, synonyms)
    write_catalog_md(
        source_rows=len(rows),
        merged_rows=sum(1 for row in rows if row["action"] == MERGE),
    )
    verify_database(taxonomy, products, synonyms)

    print(f"SQL migrazione: {MIGRATION_SQL_PATH} ({MIGRATION_SQL_PATH.stat().st_size / 1024:.1f} KB)")
    print(f"Report: {CATALOG_MD_PATH} ({CATALOG_MD_PATH.stat().st_size / 1024:.1f} KB)")
    return 0


if __name__ == "__main__":
    sys.exit(main())

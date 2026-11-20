#!/usr/bin/env python3
"""
Genera il catalogo preinstallato secondo la proposta tassonomica approvata.

Fonti:
  - proposta-tassonomia-prodotti.md
    (sezioni 3, 7, 8, 9 e 10)

Output:
  - app/src/main/assets/catalog.db
      asset Room con tassonomia, prodotti canonici e sinonimi.
  - catalogo.md
      report leggibile con tutte le categorie L1/L2/L3 e tutti i prodotti.

Uso:
  python tools/generate_catalog.py
"""
from __future__ import annotations

import sqlite3
import sys
import re
import unicodedata
from collections import OrderedDict
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PROPOSAL_PATH = ROOT / "proposta-tassonomia-prodotti.md"
ASSET_PATH = ROOT / "app" / "src" / "main" / "assets" / "catalog.db"
CATALOG_MD_PATH = ROOT / "catalogo.md"

EXPECTED_L1 = 22
EXPECTED_L2 = 100
EXPECTED_L3 = 455
EXPECTED_PRODUCTS = 892
EXPECTED_EMPTY_L3 = 64
PROPOSAL_TAXONOMY: list[tuple[str, str, str]] = []
PROPOSAL_EXCLUDED_IDS: set[int] = set()
PROPOSAL_RETIRED_IDS: set[int] = set()

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


def normalize_term(text: str) -> str:
    """Minuscolo + accent-fold, la stessa normalizzazione usata da Kotlin."""
    decomposed = unicodedata.normalize("NFKD", text.strip().lower())
    return "".join(ch for ch in decomposed if not unicodedata.combining(ch))


def _proposal_section(source: str, number: int) -> str:
    match = re.search(
        rf"^## {number}\. .*?(?=^## {number + 1}\. |^## Appendice|\Z)",
        source,
        flags=re.MULTILINE | re.DOTALL,
    )
    if match is None:
        fail(f"sezione {number} non trovata in {PROPOSAL_PATH.name}")
    return match.group(0)


def _table_cells(line: str) -> list[str]:
    return [cell.strip() for cell in line.strip().strip("|").split("|")]


def read_mapping() -> list[dict[str, object]]:
    """Legge tassonomia e assegnazioni direttamente dalla proposta."""
    global PROPOSAL_TAXONOMY, PROPOSAL_EXCLUDED_IDS, PROPOSAL_RETIRED_IDS
    if not PROPOSAL_PATH.exists():
        fail(f"proposta non trovata: {PROPOSAL_PATH}")
    source = PROPOSAL_PATH.read_text(encoding="utf-8")
    tree_section = _proposal_section(source, 3)
    keep_section = _proposal_section(source, 7)
    additions_section = _proposal_section(source, 8)
    exclusion_section = _proposal_section(source, 9)
    bio_section = _proposal_section(source, 10)

    taxonomy: list[tuple[str, str, str]] = []
    current_l1: str | None = None
    for line in tree_section.splitlines():
        if line.startswith("### "):
            current_l1 = line[4:].strip()
        elif line.startswith("|"):
            cells = _table_cells(line)
            if len(cells) == 2 and cells[0] != "L2" and not cells[0].startswith("---"):
                if not current_l1:
                    fail("riga L2 trovata prima del primo L1")
                taxonomy.extend(
                    (current_l1, cells[0], l3.strip())
                    for l3 in cells[1].split(";")
                    if l3.strip()
                )
    if not taxonomy:
        fail("albero tassonomico vuoto")
    if len(set(taxonomy)) != len(taxonomy):
        fail("triplette L1/L2/L3 duplicate nella sezione 3")
    PROPOSAL_TAXONOMY = taxonomy

    products: list[dict[str, object]] = []
    current_l1 = None
    kept_ids: set[int] = set()
    for line in keep_section.splitlines():
        if line.startswith("### "):
            current_l1 = line[4:].strip()
        elif line.startswith("|"):
            cells = _table_cells(line)
            if len(cells) == 5 and cells[0] != "L2" and cells[2].isdigit():
                product_id = int(cells[2])
                if product_id in kept_ids:
                    fail(f"ID mantenuto duplicato nella sezione 7: {product_id}")
                kept_ids.add(product_id)
                products.append({
                    "id": product_id,
                    "name": cells[3],
                    "path": (current_l1, cells[0], cells[1]),
                    "aliases": [],
                })

    additions: list[tuple[str, str, str, str]] = []
    current_l1 = None
    for line in additions_section.splitlines():
        if line.startswith("### "):
            current_l1 = line[4:].strip()
        elif line.startswith("|"):
            cells = _table_cells(line)
            if len(cells) == 3 and cells[0] != "L2" and not cells[0].startswith("---"):
                if current_l1 is not None:
                    additions.append((current_l1, cells[0], cells[1], cells[2]))

    excluded_ids = {
        int(cells[0])
        for line in exclusion_section.splitlines()
        if line.startswith("|")
        for cells in [_table_cells(line)]
        if len(cells) == 2 and cells[0].isdigit()
    }
    retired_ids = {
        int(cells[0])
        for line in bio_section.splitlines()
        if line.startswith("|")
        for cells in [_table_cells(line)]
        if len(cells) == 3 and cells[0].isdigit()
    }
    if kept_ids & excluded_ids or kept_ids & retired_ids or excluded_ids & retired_ids:
        fail("un ID originale compare in più insiemi tra mantenuti, esclusi e Bio ritirati")
    if (len(kept_ids), len(excluded_ids), len(retired_ids)) != (607, 16, 125):
        fail("conteggi inattesi negli insiemi di ID mantenuti, esclusi o Bio ritirati")
    if len(kept_ids | excluded_ids | retired_ids) != 748:
        fail("gli ID originali non sono partizionati in modo univoco")
    PROPOSAL_EXCLUDED_IDS = excluded_ids
    PROPOSAL_RETIRED_IDS = retired_ids

    used_ids = kept_ids | excluded_ids | retired_ids
    next_id = max(used_ids) + 1
    names = {str(product["name"]).casefold() for product in products}
    for l1, l2, l3, name in additions:
        path = (l1, l2, l3)
        if path not in set(taxonomy):
            fail(f"nuovo prodotto fuori albero tassonomico: {path}")
        if name.casefold() in names:
            fail(f"nome prodotto duplicato: {name}")
        names.add(name.casefold())
        products.append({"id": next_id, "name": name, "path": path, "aliases": []})
        next_id += 1

    if len(kept_ids) != 607 or len(additions) != 285:
        fail(f"conteggi proposta inattesi: {len(kept_ids)} mantenuti, {len(additions)} nuovi")
    if len(products) != EXPECTED_PRODUCTS:
        fail(f"prodotti attesi {EXPECTED_PRODUCTS}, trovati {len(products)}")

    by_name = {str(p["name"]).casefold(): p for p in products}
    # Sinonimi espliciti della tabella introduttiva della sezione 8.
    for line in additions_section.splitlines():
        if not line.startswith("|"):
            continue
        cells = _table_cells(line)
        if len(cells) == 2 and cells[0] not in ("Prodotto canonico", "---") and not cells[0].startswith("---"):
            product = by_name.get(cells[0].casefold())
            if product is None:
                fail(f"sinonimo assegnato a prodotto assente: {cells[0]}")
            product["aliases"].extend(alias.strip() for alias in cells[1].split(";") if alias.strip())

    # Le espressioni Bio storiche risolvono il prodotto base e restano attributi
    # di acquisto nel flusso di importazione; non creano ID canonici separati.
    bio_forms = ("Bio", "Biologico", "Biologica", "Biologici", "Biologiche")
    for product in products:
        name = str(product["name"])
        product["aliases"].extend(
            [f"{name} {form}" for form in bio_forms]
            + [f"{form} {name}" for form in bio_forms]
        )

    if len({int(p["id"]) for p in products}) != len(products):
        fail("collisione tra ID mantenuti e nuovi")
    return products


def build_taxonomy(rows: list[dict[str, object]]) -> dict[str, object]:
    l1_names: list[str] = []
    l2_keys: list[tuple[str, str]] = []
    l3_keys = list(PROPOSAL_TAXONOMY)
    for l1, l2, _ in l3_keys:
        if l1 not in l1_names:
            l1_names.append(l1)
        if (l1, l2) not in l2_keys:
            l2_keys.append((l1, l2))
    if (len(l1_names), len(l2_keys), len(l3_keys)) != (EXPECTED_L1, EXPECTED_L2, EXPECTED_L3):
        fail(f"dimensione tassonomia inattesa: {len(l1_names)}/{len(l2_keys)}/{len(l3_keys)}")
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
        "l2_rows": [(l2_ids[(l1, l2)], l1_ids[l1], l2, l2_sort[(l1, l2)]) for l1, l2 in l2_keys],
        "l3_rows": [(l3_ids[(l1, l2, l3)], l2_ids[(l1, l2)], l3, l3_sort[(l1, l2, l3)]) for l1, l2, l3 in l3_keys],
        "l2_ids": l2_ids,
        "l3_ids": l3_ids,
    }


def build_products(rows: list[dict[str, object]], taxonomy: dict[str, object]) -> list[dict[str, object]]:
    l3_ids: dict[tuple[str, str, str], int] = taxonomy["l3_ids"]  # type: ignore[assignment]
    products: list[dict[str, object]] = []
    for row in rows:
        path = row["path"]
        if path not in l3_ids:
            fail(f"categoria prodotto non trovata: {path}")
        products.append({
            "id": int(row["id"]),
            "name": str(row["name"]),
            "level3_id": l3_ids[path],
            "path": path,
            "aliases": list(row["aliases"]),
        })
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


def write_catalog_md(source_rows: int) -> None:
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
        "> Fonte: `proposta-tassonomia-prodotti.md` (sezioni 3, 7 e 8).",
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
        f"> Il seed contiene {format_int(source_rows)} assegnazioni prodotto canoniche; "
        f"{format_int(n_l3)} categorie L3, incluse quelle senza prodotti predefiniti.",
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

        invalid = conn.execute(
            """
            SELECT COUNT(*) FROM CANONICAL_PRODUCT
            WHERE level3_id IS NULL OR TRIM(name) = '' OR is_user_defined != 0
            """
        ).fetchone()[0]
        if invalid:
            fail(f"prodotti seed invalidi: {invalid}")

        forbidden_ids = PROPOSAL_EXCLUDED_IDS | PROPOSAL_RETIRED_IDS
        present_forbidden = conn.execute(
            "SELECT id FROM CANONICAL_PRODUCT WHERE id IN (" +
            ",".join(str(product_id) for product_id in sorted(forbidden_ids)) + ")"
        ).fetchall()
        if present_forbidden:
            fail(f"ID esclusi o Bio ritirati presenti nel seed: {[row[0] for row in present_forbidden]}")

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
        if empty_l3 != EXPECTED_EMPTY_L3:
            fail(f"categorie L3 vuote attese {EXPECTED_EMPTY_L3}, trovate {empty_l3}")

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
        print(f"  prodotti: {counts['products']}")
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
    write_catalog_md(source_rows=len(rows))
    verify_database(taxonomy, products, synonyms)

    print(f"Report: {CATALOG_MD_PATH} ({CATALOG_MD_PATH.stat().st_size / 1024:.1f} KB)")
    return 0


if __name__ == "__main__":
    sys.exit(main())

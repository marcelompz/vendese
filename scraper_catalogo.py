import csv
import json
import os
import time
from pathlib import Path
from urllib.parse import parse_qs, urljoin, urlparse

import requests
from bs4 import BeautifulSoup

BASE_URL = "https://www.cabralshop.com/"
OUTPUT_DIR = Path("salida_catalogo")
IMAGES_DIR = OUTPUT_DIR / "imagenes"
DELAY_SECONDS = 1.0

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    ),
    "Accept-Language": "es-PY,es;q=0.9,pt;q=0.8,en;q=0.7",
}


def fetch_soup(session: requests.Session, url: str) -> BeautifulSoup | None:
    try:
        resp = session.get(url, headers=HEADERS, timeout=20)
        resp.raise_for_status()
        if not resp.encoding or resp.encoding.lower() == "iso-8859-1":
            resp.encoding = resp.apparent_encoding or "utf-8"
        return BeautifulSoup(resp.text, "html.parser")
    except requests.RequestException as exc:
        print(f"[ERROR] No se pudo obtener {url}: {exc}")
        return None


def discover_categories(soup: BeautifulSoup, base_url: str) -> list[dict]:
    categories = []
    seen_urls = set()

    for link in soup.select('a[href*="produtos_category"]'):
        href = link.get("href", "").strip()
        if not href:
            continue
        full_url = urljoin(base_url, href)
        if full_url in seen_urls:
            continue
        seen_urls.add(full_url)

        name = " ".join(link.get_text(" ", strip=True).split())
        if not name:
            img = link.find("img")
            if img and img.get("alt"):
                name = img["alt"].strip()

        categories.append({
            "category_name": name or "Sin nombre",
            "category_url": full_url,
        })

    return categories


def download_image(
    session: requests.Session, img_url: str, dest_folder: Path, prefix: str
) -> str:
    if not img_url:
        return ""

    dest_folder.mkdir(parents=True, exist_ok=True)
    parsed = urlparse(img_url)
    ext = os.path.splitext(parsed.path)[1].lower()
    if ext not in {".jpg", ".jpeg", ".png", ".webp", ".gif"}:
        ext = ".jpg"

    safe_prefix = "".join(c if c.isalnum() or c in "-_" else "_" for c in prefix)[:40]
    filename = f"{safe_prefix}_{abs(hash(img_url)) % 100000}{ext}"
    filepath = dest_folder / filename

    if filepath.exists():
        return str(filepath)

    try:
        resp = session.get(img_url, headers=HEADERS, timeout=20, stream=True)
        resp.raise_for_status()
        with open(filepath, "wb") as f:
            for chunk in resp.iter_content(chunk_size=8192):
                if chunk:
                    f.write(chunk)
        return str(filepath)
    except requests.RequestException as exc:
        print(f"[WARN] Falló descarga de imagen {img_url}: {exc}")
        return ""


def parse_products_from_category(
    session: requests.Session,
    category: dict,
    download_imgs: bool = True,
    seen_ids: set[str] | None = None,
) -> list[dict]:
    soup = fetch_soup(session, category["category_url"])
    if not soup:
        return []

    if seen_ids is None:
        seen_ids = set()

    items = []
    # El sitio usa div.videos_video; solo los enlaces a produto?id_produto=
    # son productos (el resto son promociones foto?id_categoria=).
    for link in soup.select('a[href*="produto?id_produto="]'):
        href = link.get("href", "").strip()
        if not href:
            continue

        product_url = urljoin(category["category_url"], href)
        query = parse_qs(urlparse(product_url).query)
        product_id = (query.get("id_produto") or [""])[0]
        if not product_id or product_id in seen_ids:
            continue
        seen_ids.add(product_id)

        card = link.find_parent(class_="videos_video") or link.parent
        title_tag = card.find(["h1", "h2", "h3", "h4"]) if card else None
        title = " ".join(title_tag.get_text(" ", strip=True).split()) if title_tag else ""
        if not title:
            title = " ".join(link.get_text(" ", strip=True).split())

        img_tag = card.find("img") if card else link.find("img")
        img_src = (img_tag.get("src") or img_tag.get("data-src")) if img_tag else ""
        img_url = urljoin(category["category_url"], img_src) if img_src else ""

        if not title and not img_url:
            continue

        local_img_path = ""
        if download_imgs and img_url:
            local_img_path = download_image(session, img_url, IMAGES_DIR, product_id)
            time.sleep(0.3)

        items.append({
            "category": category["category_name"],
            "category_url": category["category_url"],
            "title": title,
            "product_id": product_id,
            "product_url": product_url,
            "image_url": img_url,
            "local_image_path": local_img_path,
        })

    return items


def export_json(records: list[dict], filepath: Path) -> None:
    with open(filepath, "w", encoding="utf-8") as f:
        json.dump(records, f, ensure_ascii=False, indent=2)


def export_csv(records: list[dict], filepath: Path) -> None:
    fieldnames = [
        "category",
        "category_url",
        "title",
        "product_id",
        "product_url",
        "image_url",
        "local_image_path",
    ]
    with open(filepath, "w", newline="", encoding="utf-8-sig") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(records)


def export_pos_csv(records: list[dict], filepath: Path) -> None:
    """CSV con las columnas que el importador de @vendese permite mapear.

    El sitio no publica precios: se dejan en 0 para completarlos después.
    El importador exige reference, name, barcode y precios numéricos no vacíos.
    """
    fieldnames = [
        "name",
        "reference",
        "barcode",
        "buy",
        "sell",
        "category",
        "image",
        "product_url",
    ]
    name_counts: dict[str, int] = {}
    for record in records:
        title = record["title"]
        name_counts[title] = name_counts.get(title, 0) + 1

    with open(filepath, "w", newline="", encoding="utf-8-sig") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for record in records:
            product_id = record["product_id"]
            name = record["title"]
            if name_counts[name] > 1:
                name = f"{name} [{product_id}]"
            writer.writerow({
                "name": name,
                "reference": f"CABRAL-{product_id}",
                "barcode": f"CABRAL-{product_id}",
                "buy": "0",
                "sell": "0",
                "category": record["category"],
                "image": record["local_image_path"],
                "product_url": record["product_url"],
            })


def main(download_imgs: bool = True) -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    session = requests.Session()

    home_soup = fetch_soup(session, BASE_URL)
    if not home_soup:
        return

    categories = discover_categories(home_soup, BASE_URL)
    print(f"Categorías detectadas: {len(categories)}")

    all_records = []
    seen_ids: set[str] = set()
    for cat in categories:
        print(f"Procesando categoría: {cat['category_name']} -> {cat['category_url']}")
        products = parse_products_from_category(
            session, cat, download_imgs=download_imgs, seen_ids=seen_ids
        )
        all_records.extend(products)
        time.sleep(DELAY_SECONDS)

    json_path = OUTPUT_DIR / "catalogo.json"
    csv_path = OUTPUT_DIR / "catalogo.csv"
    pos_csv_path = OUTPUT_DIR / "catalogo_pos_import.csv"

    export_json(all_records, json_path)
    export_csv(all_records, csv_path)
    export_pos_csv(all_records, pos_csv_path)

    print(f"Listo. Total de registros exportados: {len(all_records)}")
    print(f"- JSON: {json_path}")
    print(f"- CSV:  {csv_path}")
    print(f"- CSV para importar en @vendese: {pos_csv_path}")


if __name__ == "__main__":
    main(download_imgs=True)

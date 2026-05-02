import hashlib


def make_promo_id(bank: str, merchant: str | None, title: str | None) -> str:
    raw = f"{bank}|{(merchant or '').strip().lower()}|{(title or '').strip().lower()}"
    return hashlib.sha1(raw.encode('utf-8')).hexdigest()[:20]

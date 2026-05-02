from datetime import datetime, timezone

import firebase_admin
from firebase_admin import credentials, firestore
from google.cloud.firestore_v1 import SERVER_TIMESTAMP
from tenacity import retry, stop_after_attempt, wait_exponential

from .base import PromotionStorage
from utils.promo_id import make_promo_id

_BATCH_LIMIT = 500


class FirestoreStorage(PromotionStorage):
    def __init__(self, project_id: str):
        if not firebase_admin._apps:
            firebase_admin.initialize_app(
                credentials.ApplicationDefault(),
                {'projectId': project_id},
            )
        self._db = firestore.client()
        self._collection = self._db.collection('promotions')

    def save(self, bank_slug: str, promotions: list[dict]) -> None:
        run_id = datetime.now(tz=timezone.utc).strftime('%Y-%m-%d_%H-%M-%S')
        now = datetime.now(tz=timezone.utc)

        entries = [
            (make_promo_id(p.get('bank', bank_slug), p.get('merchant'), p.get('title')), p)
            for p in promotions
        ]

        # Single RPC to determine which docs are new (need first_seen_at set)
        doc_refs = [self._collection.document(doc_id) for doc_id, _ in entries]
        existing_ids = {snap.id for snap in self._db.get_all(doc_refs) if snap.exists}

        batch = self._db.batch()
        ops = 0
        written = 0

        for doc_id, promo in entries:
            doc_ref = self._collection.document(doc_id)
            doc_data = {
                **promo,
                'promo_id': doc_id,
                'bank_slug': bank_slug,
                'is_active': True,
                'last_seen_at': SERVER_TIMESTAMP,
                'scraped_at': SERVER_TIMESTAMP,
                'run_id': run_id,
            }
            if doc_id not in existing_ids:
                doc_data['first_seen_at'] = now

            batch.set(doc_ref, doc_data, merge=True)
            ops += 1
            written += 1

            if ops == _BATCH_LIMIT:
                self._commit_batch(batch)
                batch = self._db.batch()
                ops = 0

        if ops > 0:
            self._commit_batch(batch)

        print(f"Wrote {written} docs to Firestore promotions/ for bank={bank_slug}")

    def finalize_bank(self, bank_slug: str, run_started_at: datetime) -> None:
        stale = (
            self._collection
            .where('bank_slug', '==', bank_slug)
            .where('is_active', '==', True)
            .where('last_seen_at', '<', run_started_at)
            .stream()
        )

        batch = self._db.batch()
        ops = 0
        deactivated = 0

        for doc in stale:
            batch.update(doc.reference, {'is_active': False})
            ops += 1
            deactivated += 1
            if ops == _BATCH_LIMIT:
                self._commit_batch(batch)
                batch = self._db.batch()
                ops = 0

        if ops > 0:
            self._commit_batch(batch)

        print(f"Soft-deleted {deactivated} stale docs for bank={bank_slug}")

    @retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=2, max=30))
    def _commit_batch(self, batch) -> None:
        batch.commit()

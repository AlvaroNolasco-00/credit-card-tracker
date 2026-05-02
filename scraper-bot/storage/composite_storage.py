from datetime import datetime

from .base import PromotionStorage


class CompositeStorage(PromotionStorage):
    def __init__(self, storages: list[PromotionStorage]):
        self._storages = storages

    def save(self, bank_slug: str, promotions: list[dict]) -> None:
        for storage in self._storages:
            storage.save(bank_slug, promotions)

    def finalize_bank(self, bank_slug: str, run_started_at: datetime) -> None:
        for storage in self._storages:
            storage.finalize_bank(bank_slug, run_started_at)

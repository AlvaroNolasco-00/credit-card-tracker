from abc import ABC, abstractmethod
from datetime import datetime


class PromotionStorage(ABC):
    @abstractmethod
    def save(self, bank_slug: str, promotions: list[dict]) -> None:
        pass

    def finalize_bank(self, bank_slug: str, run_started_at: datetime) -> None:
        pass

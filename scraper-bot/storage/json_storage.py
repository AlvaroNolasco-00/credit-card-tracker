import json
import os
from datetime import datetime

from .base import PromotionStorage


class JsonStorage(PromotionStorage):
    def __init__(self, output_dir: str = 'output'):
        self.output_dir = output_dir
        os.makedirs(self.output_dir, exist_ok=True)

    def save(self, bank_slug: str, promotions: list[dict]) -> None:
        ts = datetime.now().strftime('%Y-%m-%d_%H-%M-%S')
        filename = f"{bank_slug}_{ts}.json"
        filepath = os.path.join(self.output_dir, filename)

        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump({
                'last_updated': datetime.now().isoformat(),
                'run_timestamp': ts,
                'total_promotions': len(promotions),
                'promotions': promotions,
            }, f, indent=4, ensure_ascii=False)

        print(f"Saved {len(promotions)} promotions to {filepath}")

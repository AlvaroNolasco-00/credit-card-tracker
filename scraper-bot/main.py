import requests
import json
import os
import re
from datetime import datetime, timedelta
from playwright.sync_api import sync_playwright

class BankScraper:
    def __init__(self):
        self.headers = {
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            'Accept': 'application/json, text/plain, */*',
            'Referer': 'https://www.bancoagricola.com/promociones'
        }
        self.output_dir = 'output'
        if not os.path.exists(self.output_dir):
            os.makedirs(self.output_dir)

    def scrape_banco_agricola(self):
        print("Scraping Banco Agricola...")
        url = "https://www.bancoagricola.com/com/promociones/promociones_get?segmento=principal"
        
        try:
            response = requests.get(url, headers=self.headers)
            response.raise_for_status()
            data = response.json()
            
            # Simple normalization of the data
            promotions_raw = []
            if isinstance(data, list):
                promotions_raw = data
            elif isinstance(data, dict) and 'promociones' in data:
                promotions_raw = data['promociones']
            
            promotions = []
            for item in promotions_raw:
                if isinstance(item, dict):
                    promo = {
                        'bank': 'Banco Agricola',
                        'merchant': item.get('nombre_comercio'),
                        'title': item.get('nombre_promocion'),
                        'description': item.get('descripcion_promocion'),
                        'category': item.get('categoria'),
                        'valid_until': item.get('vigencia_hasta'),
                        'days': item.get('dias_promo'),
                        'benefit': item.get('beneficio'),
                        'image': item.get('imagen_peq')
                    }
                    promotions.append(promo)
            
            self.save_json('agricola.json', promotions)
            return promotions
            
        except Exception as e:
            print(f"Error scraping Banco Agricola: {e}")
            return []

    def scrape_banco_cuscatlan(self):
        print("Scraping Banco Cuscatlan via Playwright interception...")
        promotions = []
        
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_page()
            
            def handle_response(response):
                nonlocal promotions
                if "webapi" in response.url and response.status == 200:
                    try:
                        data = response.json()
                        if 'data' in data and 'promocions' in data['data'] and 'data' in data['data']['promocions']:
                            promos_raw = data['data']['promocions']['data']
                            
                            for item in promos_raw:
                                attributes = item.get('attributes', {})

                                # Real field names discovered from the raw JSON
                                card = attributes.get('card') or {}
                                business = (attributes.get('business') or {}).get('data') or {}
                                business_attrs = business.get('attributes') or {}
                                tags = attributes.get('tags', {}).get('data', [])
                                category = tags[0].get('attributes', {}).get('description') if tags else None
                                logo_data = (business_attrs.get('logo') or {}).get('data') or {}
                                logo_attrs = logo_data.get('attributes') or {}
                                card_image = (card.get('imagen') or {}).get('data') or {}
                                card_image_url = (card_image.get('attributes') or {}).get('url')

                                promo = {
                                    'bank': 'Banco Cuscatlan',
                                    'merchant': business_attrs.get('name'),
                                    'title': card.get('title'),
                                    'description': card.get('description'),
                                    'category': category,
                                    'valid_until': attributes.get('date_end'),
                                    'days': None,
                                    'benefit': None,
                                    'image': card_image_url or logo_attrs.get('url')
                                }
                                promotions.append(promo)
                    except:
                        pass


            page.on("response", handle_response)
            
            try:
                page.goto("https://www.bancocuscatlan.com/tarjetas/promociones", wait_until="networkidle", timeout=30000)
                page.wait_for_timeout(3000) # Wait a bit for GraphQL calls to finish
            except Exception as e:
                print(f"Error navigating: {e}")
            
            browser.close()
            
        self.save_json('cuscatlan.json', promotions)
        return promotions

    def scrape_banco_bac(self):
        print("Scraping BAC Credomatic via Playwright DOM Parsing...")
        promotions = []
        
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_page()
            
            try:
                page.goto("https://www.baccredomatic.com/es-sv/personas/promociones", wait_until="networkidle", timeout=30000)
                # Wait for the cards to load
                page.wait_for_selector(".real-estate-card", timeout=15000)
                
                # We can scroll down a bit to trigger lazy loading if necessary
                for _ in range(5):
                    page.mouse.wheel(0, 1000)
                    page.wait_for_timeout(500)
                
                # Extract DOM data
                cards = page.locator(".real-estate-card").all()
                print(f"Found {len(cards)} promotional cards on BAC.")
                
                for card in cards:
                    try:
                        title_el = card.locator(".h2 span").first
                        title = title_el.inner_text().strip() if title_el.count() > 0 else None
                        
                        desc_el = card.locator(".real-estate-card--body p:nth-of-type(2)").first
                        description = desc_el.inner_text().strip() if desc_el.count() > 0 else None
                        
                        date_el = card.locator(".red-background-text").first
                        valid_until = date_el.inner_text().strip() if date_el.count() > 0 else None
                        
                        img_el = card.locator(".real-estate-card--header img").first
                        image = img_el.get_attribute("src") if img_el.count() > 0 else None
                        
                        promo = {
                            'bank': 'BAC Credomatic',
                            'merchant': title, # BAC often bundles merchant and title
                            'title': title,
                            'description': description,
                            'category': None,
                            'valid_until': valid_until,
                            'days': None,
                            'benefit': None,
                            'image': f"https://www.baccredomatic.com{image}" if image and image.startswith("/") else image
                        }
                        promotions.append(promo)
                    except Exception as e:
                        print(f"Failed parsing a card: {e}")
                        
            except Exception as e:
                print(f"Error navigating BAC: {e}")
            
            browser.close()
            
        self.save_json('bac.json', promotions)
        return promotions

    def scrape_banco_promerica(self):
        print("Scraping Banco Promerica via Playwright DOM Parsing...")
        promotions = []
        
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_page()
            
            try:
                # Load list view to get better descriptions and dates in text
                page.goto("https://www.clubpromerica.com/elsalvador/promociones-del-mes?viewmode=list", wait_until="networkidle", timeout=30000)
                page.wait_for_selector(".item-box", timeout=15000)
                
                # Expand items if lazy loaded
                for _ in range(5):
                    page.mouse.wheel(0, 1000)
                    page.wait_for_timeout(500)
                
                cards = page.locator(".item-box").all()
                print(f"Found {len(cards)} promotional cards on Promerica.")
                
                for card in cards:
                    try:
                        title_el = card.locator(".product-title a").first
                        title = title_el.inner_text().strip() if title_el.count() > 0 else None
                        
                        img_el = card.locator(".picture img").first
                        image = img_el.get_attribute("src") if img_el.count() > 0 else None
                        
                        # Get short description (not the title duplicated)
                        desc_el = card.locator(".short-description").first
                        short_desc = desc_el.inner_text().strip() if desc_el.count() > 0 else None
                        
                        # Get validity from the .add-info or the full .details block
                        details_el = card.locator(".details").first
                        details_text = details_el.inner_text().strip() if details_el.count() > 0 else ""
                        
                        # Strip the title from the details text to get only description
                        description = short_desc
                        if not description and details_text and title:
                            description = details_text.replace(title, '').strip()
                        
                        # Try to find date from details text using regex
                        date_match = re.search(r'(\d{1,2}\s+de\s+\w+\s+(?:al|de)\s+\d{1,2}\s+de\s+\w+(?:\s+de\s+\d{4})?|[Dd]el\s+\d{1,2}\s+al\s+\d{1,2}\s+de\s+\w+|[Ll]os\s+\w+\s+de\s+\w+)', details_text)
                        valid_until = date_match.group(0).strip() if date_match else None
                        
                        promo = {
                            'bank': 'Banco Promerica',
                            'merchant': title,
                            'title': title,
                            'description': description,
                            'category': None,
                            'valid_until': valid_until,
                            'days': None,
                            'benefit': None,
                            'image': image
                        }
                        promotions.append(promo)
                    except Exception as e:
                        print(f"Failed parsing Promerica card: {e}")
                        
            except Exception as e:
                print(f"Error navigating Promerica: {e}")
            
            browser.close()
            
        self.save_json('promerica.json', promotions)
        return promotions

    def scrape_banco_davivienda(self):
        print("Scraping Banco Davivienda via Playwright DOM Parsing...")
        promotions = []
        
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_page()
            
            try:
                page.goto("https://promociones.davivienda.com.sv/", wait_until="networkidle", timeout=30000)
                page.wait_for_selector(".wpupg-item", timeout=15000)
                
                # Scroll a bit if necessary to load grid
                for _ in range(3):
                    page.mouse.wheel(0, 1000)
                    page.wait_for_timeout(1000)
                    
                cards = page.locator(".wpupg-item").all()
                print(f"Found {len(cards)} promotional cards on Davivienda.")
                
                for card in cards:
                    try:
                        title_el = card.locator(".wpupg-item-title").first
                        title = title_el.inner_text().strip() if title_el.count() > 0 else None
                        
                        img_el = card.locator("img").first
                        image = img_el.get_attribute("src") if img_el.count() > 0 else None
                        
                        # Get the detail page link to extract description and dates
                        link_el = card.locator("a").first
                        detail_url = link_el.get_attribute("href") if link_el.count() > 0 else None
                        
                        description = None
                        valid_until = None
                        category = None
                        
                        if detail_url:
                            try:
                                detail_page = browser.new_page()
                                detail_page.goto(detail_url, wait_until="domcontentloaded", timeout=15000)
                                
                                # Get description from the single-post p elements (skip empty ones)
                                desc_parts = []
                                for p_el in detail_page.locator(".single-post p").all():
                                    text = p_el.inner_text().strip()
                                    if text:
                                        desc_parts.append(text)
                                description = ' '.join(desc_parts[:2]) if desc_parts else None
                                
                                # Try to extract valid_until from full page text
                                content_text = ' '.join(desc_parts)
                                date_match = re.search(r'[Hh]asta\s+(?:el\s+)?(\d{1,2}\s+de\s+\w+(?:\s+de\s+\d{4})?)', content_text)
                                valid_until = date_match.group(1).strip() if date_match else None
                                
                                # Category from WP taxonomy
                                cat_el = detail_page.locator(".wpupg-tax-recipe_tag a, .cat-links a, .entry-meta a").first
                                category = cat_el.inner_text().strip() if cat_el.count() > 0 else None
                                
                                detail_page.close()
                            except:
                                pass
                        
                        promo = {
                            'bank': 'Banco Davivienda',
                            'merchant': title,
                            'title': title,
                            'description': description,
                            'category': category,
                            'valid_until': valid_until,
                            'days': None,
                            'benefit': None,
                            'image': image
                        }
                        if promo['title'] or promo['image']:
                            promotions.append(promo)
                            
                    except Exception as e:
                        print(f"Failed parsing Davivienda card: {e}")
                        
            except Exception as e:
                print(f"Error navigating Davivienda: {e}")
            
            browser.close()
            
        self.save_json('davivienda.json', promotions)
        return promotions

    def scrape_banco_credicomer(self):
        print("Scraping Banco Credicomer via Playwright DOM Parsing...")
        promotions = []
        
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_page()
            
            try:
                page.goto("https://www.credicomer.com.sv/personas/promociones", wait_until="networkidle", timeout=30000)
                page.wait_for_selector(".card.bg-white", timeout=15000)
                
                # Expand items if lazy loaded
                for _ in range(5):
                    page.mouse.wheel(0, 1000)
                    page.wait_for_timeout(500)
                
                cards = page.locator(".card.bg-white").all()
                print(f"Found {len(cards)} promotional cards on Credicomer.")
                
                for card in cards:
                    try:
                        title_el = card.locator(".text-lg.font-semibold").first
                        title = title_el.inner_text().strip() if title_el.count() > 0 else None
                        
                        img_el = card.locator("img").first
                        image_src = img_el.get_attribute("src") if img_el.count() > 0 else None
                        
                        # Fix relative image URLs
                        if image_src and image_src.startswith('/'):
                            image = f"https://www.credicomer.com.sv{image_src}"
                        else:
                            image = image_src
                            
                        # Get the raw text after removing the title
                        raw_text = card.inner_text()
                        raw_no_title = raw_text.replace(title, '').strip() if title and title in raw_text else raw_text
                        
                        # Parse countdown: "Termina en\n14 dias, HH:MM:SS\nVer Condiciones"
                        valid_until = None
                        expires_match = re.search(r'Termina en\s+(\d+)\s*dias?,', raw_no_title, re.IGNORECASE)
                        if expires_match:
                            days_left = int(expires_match.group(1))
                            valid_until = (datetime.now() + timedelta(days=days_left)).strftime('%Y-%m-%d')
                        
                        # Clean description: remove the entire countdown block and "Ver Condiciones"
                        clean_desc = re.sub(r'Termina en\s+\d+\s*dias?,\s*[\d:]+\s*Ver Condiciones', '', raw_no_title, flags=re.IGNORECASE)
                        clean_desc = " ".join(clean_desc.split()).strip() if clean_desc else None
                        
                        promo = {
                            'bank': 'Credicomer',
                            'merchant': title, 
                            'title': title,
                            'description': clean_desc if clean_desc else None,
                            'category': None,
                            'valid_until': valid_until, 
                            'days': None,
                            'benefit': None,
                            'image': image
                        }
                        if promo['title']:
                            promotions.append(promo)
                    except Exception as e:
                        print(f"Failed parsing Credicomer card: {e}")
                        
            except Exception as e:
                print(f"Error navigating Credicomer: {e}")
            
            browser.close()
            
        self.save_json('credicomer.json', promotions)
        return promotions

    def save_json(self, filename, data):
        # Add timestamp to filename: e.g. agricola_2026-04-16_14-30-00.json
        ts = datetime.now().strftime('%Y-%m-%d_%H-%M-%S')
        name, ext = os.path.splitext(filename)
        timestamped_filename = f"{name}_{ts}{ext}"
        filepath = os.path.join(self.output_dir, timestamped_filename)
        
        now_iso = datetime.now().isoformat()
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump({
                'last_updated': now_iso,
                'run_timestamp': ts,
                'total_promotions': len(data),
                'promotions': data
            }, f, indent=4, ensure_ascii=False)
        print(f"Saved {len(data)} promotions to {filepath}")

if __name__ == "__main__":
    scraper = BankScraper()
    scraper.scrape_banco_agricola()
    scraper.scrape_banco_cuscatlan()
    scraper.scrape_banco_bac()
    scraper.scrape_banco_promerica()
    scraper.scrape_banco_davivienda()
    scraper.scrape_banco_credicomer()

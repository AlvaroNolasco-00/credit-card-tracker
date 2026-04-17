from playwright.sync_api import sync_playwright
import json

def debug_promerica():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        
        found = []

        def handle_response(response):
            if response.status == 200 and "json" in response.headers.get("content-type", ""):
                try:
                    data = response.json()
                    # Just print URLs to see what JSONs are loaded
                    print(f"JSON detectado de: {response.url}")
                    if isinstance(data, dict):
                        print(" Keys:", list(data.keys()))
                    elif isinstance(data, list):
                        print(f" List items: {len(data)}")
                except:
                    pass

        page.on("response", handle_response)
        print("Navegando a https://www.clubpromerica.com/el-salvador/categorias/")
        page.goto("https://www.clubpromerica.com/el-salvador/categorias/", wait_until="networkidle", timeout=45000)
        page.wait_for_timeout(3000)
        
        # Scroll to load elements
        for _ in range(3):
            page.mouse.wheel(0, 1000)
            page.wait_for_timeout(1000)
            
        print("Buscando en el DOM posibles tarjetas...")
        for el in page.query_selector_all('h3, .title, .card'):
            print("-", el.inner_text().strip()[:100])
            
        browser.close()

if __name__ == "__main__":
    debug_promerica()

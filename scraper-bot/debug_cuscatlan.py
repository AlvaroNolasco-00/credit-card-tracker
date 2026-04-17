from playwright.sync_api import sync_playwright
import json

def debug_cuscatlan():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        
        found = []

        def handle_response(response):
            if "webapi" in response.url and response.status == 200:
                try:
                    data = response.json()
                    if 'data' in data and 'promocions' in data['data'] and 'data' in data['data']['promocions']:
                        promos_raw = data['data']['promocions']['data']
                        if promos_raw and len(found) == 0:
                            found.append(promos_raw)
                except:
                    pass

        page.on("response", handle_response)
        page.goto("https://www.bancocuscatlan.com/tarjetas/promociones", wait_until="networkidle", timeout=30000)
        page.wait_for_timeout(3000)
        browser.close()

        if found:
            # Print the first two items in pretty JSON to understand the real structure
            print(json.dumps(found[0][:2], indent=2, ensure_ascii=False))
        else:
            print("No se encontró data.")

if __name__ == "__main__":
    debug_cuscatlan()

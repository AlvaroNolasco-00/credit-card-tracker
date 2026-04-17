from playwright.sync_api import sync_playwright

def test_bac():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        print("Escuchando peticiones de BAC...")
        
        def handle_response(response):
            if response.status == 200 and "json" in response.headers.get("content-type", ""):
                if "baccredomatic" in response.url or "promociones" in response.url or "api" in response.url:
                    print(f"JSON detectado de: {response.url}")
                    try:
                        data = response.json()
                        if isinstance(data, dict):
                            print("Keys:", list(data.keys()))
                        elif isinstance(data, list):
                            print(f"List de tamaño {len(data)}")
                    except:
                        pass

        page.on("response", handle_response)
        
        print("Navegando a https://www.baccredomatic.com/es-sv/promociones")
        page.goto("https://www.baccredomatic.com/es-sv/promociones", wait_until="networkidle", timeout=45000)
        page.wait_for_timeout(5000)
        browser.close()

if __name__ == "__main__":
    test_bac()

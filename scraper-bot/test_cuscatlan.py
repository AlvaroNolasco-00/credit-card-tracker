from playwright.sync_api import sync_playwright

def test_cuscatlan():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        print("Escuchando peticiones a /webapi/...")
        
        def handle_response(response):
            if "webapi" in response.url and response.status == 200:
                print(f"Interceptada respuesta de: {response.url}")
                try:
                    data = response.json()
                    if 'data' in data and 'promocions' in data['data']:
                        promocions_data = data['data']['promocions']
                        print("Promocions type:", type(promocions_data))
                        if isinstance(promocions_data, dict):
                            print("Promocions keys:", promocions_data.keys())
                            if 'data' in promocions_data:
                                print("Real data items:", len(promocions_data['data']))
                        elif isinstance(promocions_data, list):
                            print("Items count:", len(promocions_data))
                except:
                    pass

        page.on("response", handle_response)
        
        print("Navegando a https://www.bancocuscatlan.com/tarjetas/promociones")
        page.goto("https://www.bancocuscatlan.com/tarjetas/promociones", wait_until="networkidle")
        page.wait_for_timeout(5000)
        
        browser.close()

if __name__ == "__main__":
    test_cuscatlan()

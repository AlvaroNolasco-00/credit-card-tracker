# Manual de Uso — Bank Promotions Scraper

> Script de Python para extraer promociones de tarjetas de crédito de bancos de El Salvador.

---

## Estructura del Proyecto

```
scraper-bot/
├── main.py             # Script principal con la lógica de los 6 bancos
├── requirements.txt    # Dependencias (requests, playwright)
└── output/             # Carpeta de resultados (auto-generada)
    └── agricola_2026-04-16_16-57-23.json # Ejemplo con timestamp
```

---

## Requisitos

- **Python 3.9+** instalado.
- **Playwright** para la automatización de navegadores (requerido para Cuscatlán, BAC, Promerica, Davivienda y Credicomer).
- La librería `requests` para Banco Agrícola.

---

## Instalación

Desde la raíz del directorio `scraper-bot/`, ejecuta:

```bash
# Instalar dependencias de Python
pip3 install -r requirements.txt

# Instalar navegadores de Playwright
python3 -m playwright install chromium
```

---

## Cómo Ejecutar

```bash
cd scraper-bot
python3 main.py
```

```
Scraping Banco Agricola...
Saved 41 promotions to output/agricola_2026-04-16_16-55-33.json
...
Scraping Banco Credicomer via Playwright DOM Parsing...
Saved 15 promotions to output/credicomer_2026-04-16_16-56-13.json
```

---

## Estructura del Output (JSON)

Cada archivo generado en `output/` sigue este esquema:

```json
{
    "last_updated": "2026-04-16T16:55:33.940573",
    "run_timestamp": "2026-04-16_16-55-33",
    "total_promotions": 41,
    "promotions": [
        {
            "bank": "Banco Agricola",
            "merchant": "Cinemark",
            "title": "Vive el cine al máximo en Cinemark! Disfruta tu beneficio en entradas al 2x1",
            "description": null,
            "category": null,
            "valid_until": null,
            "days": "1,2,3,4",
            "benefit": null,
            "image": null
        }
    ]
}
```

### Descripción de Campos

| Campo | Descripción |
| :--- | :--- |
| `last_updated` | Fecha y hora ISO 8601 de la ejecución |
| `run_timestamp` | Marca de tiempo legible usada en el nombre del archivo |
| `total_promotions` | Número total de promociones extraídas |
| `bank` | Nombre del banco de origen |
| `merchant` | Nombre del comercio con la promoción |
| `title` | Título o nombre de la promoción |
| `description` | Descripción detallada (puede ser `null`) |
| `category` | Categoría del comercio, ej. Restaurantes, Viajes (puede ser `null`) |
| `valid_until` | Fecha de vencimiento de la promoción (puede ser `null`) |
| `days` | Días de vigencia codificados numéricamente (ver tabla abajo) |
| `benefit` | Descripción del beneficio, ej. "20% de descuento" (puede ser `null`) |
| `image` | URL de la imagen del comercio (puede ser `null`) |

### Tabla de Días de Vigencia

El campo `days` es una cadena con números separados por comas, donde cada número representa un día de la semana:

| Número | Día |
| :---: | :--- |
| `0` | Domingo |
| `1` | Lunes |
| `2` | Martes |
| `3` | Miércoles |
| `4` | Jueves |
| `5` | Viernes |
| `6` | Sábado |

> **Ejemplo:** `"days": "1,2,3,4,5,6,0"` significa que la promoción aplica todos los días de la semana.

---

## Cómo Extender el Script para Otro Banco

El scraper está diseñado para crecer. Para agregar un banco nuevo, sigue estos pasos:

### 1. Inspeccionar el API del banco

Abre el portal de promociones del banco en Chrome, presiona `F12` para abrir DevTools, ve a la pestaña **Network** y filtra por `Fetch/XHR`. Recarga la página y busca las peticiones que devuelvan JSON con las promociones.

### 2. Agregar un nuevo método a la clase `BankScraper`

Copia el método existente como base:

```python
def scrape_banco_nuevo(self):
    print("Scraping Banco Nuevo...")
    url = "https://www.banconuevo.com/api/promociones"  # <-- reemplaza con el endpoint real

    try:
        response = requests.get(url, headers=self.headers)
        response.raise_for_status()
        data = response.json()

        promotions_raw = []
        if isinstance(data, list):
            promotions_raw = data
        elif isinstance(data, dict) and 'promociones' in data:
            promotions_raw = data['promociones']

        promotions = []
        for item in promotions_raw:
            if isinstance(item, dict):
                promo = {
                    'bank': 'Banco Nuevo',
                    'merchant': item.get('nombre_comercio'),   # <-- ajusta las llaves según el JSON real
                    'title': item.get('nombre_promocion'),
                    'description': item.get('descripcion_promocion'),
                    'category': item.get('categoria'),
                    'valid_until': item.get('vigencia_hasta'),
                    'days': item.get('dias_promo'),
                    'benefit': item.get('beneficio'),
                    'image': item.get('imagen_peq')
                }
                promotions.append(promo)

        self.save_json('banco_nuevo.json', promotions)
        return promotions

    except Exception as e:
        print(f"Error scraping Banco Nuevo: {e}")
        return []
```

### 3. Llamar el método nuevo desde el bloque `__main__`

```python
if __name__ == "__main__":
    scraper = BankScraper()
    scraper.scrape_banco_agricola()
    scraper.scrape_banco_nuevo()  # <-- agregar aquí
```

---

## Cobertura de Bancos (100% Completado)

| Banco | Portal de Promociones | Estado |
| :--- | :--- | :---: |
| Banco Agrícola | [bancoagricola.com/promociones](https://www.bancoagricola.com/promociones) | ✅ Implementado |
| Banco Cuscatlán | [bancocuscatlan.com/promociones](https://www.bancocuscatlan.com/promociones) | ✅ Implementado |
| BAC Credomatic | [baccredomatic.com/es-sv/promociones](https://www.baccredomatic.com/es-sv/promociones) | ✅ Implementado |
| Banco Promerica | [promerica.com.sv/promociones/](https://www.promerica.com.sv/promociones/) | ✅ Implementado |
| Banco Davivienda | [promociones.davivienda.sv/](https://promociones.davivienda.sv/) | ✅ Implementado |
| Credicomer | [credicomer.com.sv/](https://www.credicomer.com.sv/) | ✅ Implementado |

---

## Errores Comunes

| Error | Causa Probable | Solución |
| :--- | :--- | :--- |
| `ModuleNotFoundError: No module named 'requests'` | Dependencia no instalada | Ejecutar `pip3 install -r requirements.txt` |
| `Error scraping Banco Agricola: 404` | El endpoint cambió de URL | Inspeccionar el Network Tab del banco nuevamente |
| `Error scraping Banco Agricola: JSONDecodeError` | La respuesta no es JSON (posiblemente HTML de bloqueo) | Revisar/actualizar el `User-Agent` en `self.headers` |
| `Saved 0 promotions` | Las llaves del JSON son distintas | Imprimir `data.keys()` para identificar la estructura real |

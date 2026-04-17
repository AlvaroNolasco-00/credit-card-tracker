# Investigación para Implementación de Scraper de Promociones Bancarias (El Salvador)

Este documento detalla los hallazgos iniciales y la estrategia propuesta para el desarrollo de un scraper encargado de recopilar promociones de tarjetas de crédito en El Salvador.

## 1. Instituciones Financieras Objetivo
Para una cobertura completa del mercado salvadoreño, se han identificado los siguientes portales de promociones:

| Banco | URL de Promociones |
| :--- | :--- |
| **Banco Agrícola** | [bancoagricola.com/promociones](https://www.bancoagricola.com/promociones) |
| **Banco Cuscatlán** | [bancocuscatlan.com/promociones](https://www.bancocuscatlan.com/promociones) |
| **BAC Credomatic** | [baccredomatic.com/es-sv/promociones](https://www.baccredomatic.com/es-sv/promociones) |
| **Banco Promerica** | [promerica.com.sv/promociones/](https://www.promerica.com.sv/promociones/) |
| **Banco Davivienda** | [promociones.davivienda.sv/](https://promociones.davivienda.sv/) |
| **Credicomer** | [credicomer.com.sv/](https://www.credicomer.com.sv/) |

## 2. Hallazgos Técnicos
Tras realizar pruebas iniciales de descarga estática en el portal de Banco Agrícola, se determinó lo siguiente:
- **Arquitectura Dinámica:** Los sitios web modernos de los bancos funcionan mayoritariamente como *Single Page Applications* (SPA).
- **Carga Asíncrona:** El contenido de las promociones no reside en el HTML inicial. Se carga mediante peticiones JavaScript (XHR/Fetch) a servicios backend una vez que el navegador ha renderizado la estructura base.
- **Desafío:** Un scraper tradicional (como `urllib` o `BeautifulSoup` básico) solo verá el menú y el pie de página, pero no los datos de las promociones.

## 3. Estrategias de Implementación Propuestas

### Opción A: Ingeniería Inversa de APIs (Recomendado)
Consiste en interceptar las llamadas que hace el sitio web a sus propios servidores de datos.
- **Pros:** Extremadamente rápido, menor consumo de ancho de banda, devuelve datos limpios (JSON).
- **Cons:** Requiere investigación manual inicial en las herramientas de desarrollador (Network tab).

### Opción B: Automatización de Navegador (Playwright/Selenium)
Simula a un usuario real abriendo un navegador (sin interfaz), esperando a que se carguen los scripts y extrayendo la información del DOM final.
- **Pros:** Más fácil de implementar para sitios con protecciones complejas o donde la API es difícil de descifrar.
- **Cons:** Más lento y requiere más recursos (CPU/RAM).

## 4. Próximos Pasos Recomendados
1. **Configuración del Entorno:** Inicializar un entorno de Python en el proyecto.
2. **Prueba de Concepto (PoC):** Intentar extraer datos de un solo banco (ej. Banco Agrícola) usando el enfoque de API para validar la facilidad de obtención de datos.
3. **Escalado:** Definir un modelo de datos común para normalizar las promociones de distintos bancos.

package com.alvaronolasco.creditcardtracker.data.entity

enum class SupportedBank(val id: String, val displayName: String, val promotionsUrl: String) {
    BANCO_AGRICOLA("banco_agricola", "Banco Agrícola", "https://www.bancoagricola.com/promociones"),
    BANCO_CUSCATLAN("banco_cuscatlan", "Banco Cuscatlán", "https://www.bancocuscatlan.com/promociones"),
    BAC_CREDOMATIC("bac_credomatic", "BAC Credomatic", "https://www.baccredomatic.com/es-sv/promociones"),
    BANCO_PROMERICA("banco_promerica", "Banco Promerica", "https://www.promerica.com.sv/promociones/"),
    BANCO_DAVIVIENDA("banco_davivienda", "Banco Davivienda", "https://promociones.davivienda.sv/"),
    CREDICOMER("credicomer", "Credicomer", "https://www.credicomer.com.sv/");

    companion object {
        fun fromId(id: String?): SupportedBank? = entries.firstOrNull { it.id == id }

        fun fromDisplayName(name: String?): SupportedBank? =
            entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) }
    }
}

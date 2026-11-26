package coach.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Contrato v2 (ClaudeCoach/contract/panel-v2.schema.json). Campos opcionales con valor por defecto:
// lo que la app no conoce se ignora; lo que falta y es obligatorio da LoadError.Corrupt.

/** Sobre cifrado v1: AES-256-GCM con clave PBKDF2-SHA256. */
@Serializable
data class Envelope(
    val v: Int,
    @SerialName("iter") val iterations: Int,
    val salt: String,
    val iv: String,
    val aad: String,
    val ct: String,
)

@Serializable
data class Payload(
    @SerialName("schema_version") val schemaVersion: Int,
    val metricas: Metricas,
    val diario: List<Dia>,
    val estado: Map<String, Estado> = emptyMap(),
)

/** Estado de una fuente. status.json guarda también apuntes sin "ok" (p. ej. hevy_cuota): se leen como no OK. */
@Serializable
data class Estado(
    val ok: Boolean = false,
    @SerialName("ultimo_intento") val ultimoIntento: String? = null,
    @SerialName("ultima_ok") val ultimaOk: String? = null,
)

@Serializable
data class Metricas(
    val fecha: String,
    @SerialName("semana_actual") val semanaActual: String,
    @SerialName("frescura_datos") val frescura: Map<String, String?> = emptyMap(),
    val fuerza: Fuerza,
    val carrera: Carrera,
    val carga: Carga,
    val recuperacion: Recuperacion,
    val composicion: Composicion = Composicion(),
    val nutricion: Nutricion? = null,
    val objetivo: Objetivo = Objetivo(),
    val umbrales: Umbrales = Umbrales(),
    val preparacion: Preparacion = Preparacion(),
    val alertas: List<String> = emptyList(),
)

@Serializable
enum class Nivel {
    @SerialName("verde") VERDE,
    @SerialName("azul") AZUL,
    @SerialName("amarillo") AMARILLO,
    @SerialName("naranja") NARANJA,
    @SerialName("rojo") ROJO,
}

@Serializable
data class Preparacion(
    val puntuacion: Int? = null,
    val nivel: Nivel? = null,
    val motivos: List<Motivo> = emptyList(),
    @SerialName("datos_suficientes") val datosSuficientes: Boolean = false,
)

@Serializable
data class Motivo(val senal: String, val resta: Int, val detalle: String)

@Serializable
data class Umbrales(val acwr: Acwr = Acwr())

@Serializable
data class Acwr(
    @SerialName("min_ok") val minOk: Double = 0.8,
    @SerialName("max_ok") val maxOk: Double = 1.3,
    val pico: Double = 1.5,
)

@Serializable
data class Fuerza(
    val semanas: List<SemanaFuerza>,
    @SerialName("semana_completa") val semanaCompleta: String,
    @SerialName("volumen_vs_objetivo") val volumen: Map<String, Volumen> = emptyMap(),
    @SerialName("ejercicios_sin_mapear") val sinMapear: Map<String, Int> = emptyMap(),
    val e1rm: List<E1rm> = emptyList(),
)

@Serializable
data class SemanaFuerza(
    val semana: String,
    val sesiones: Int = 0,
    @SerialName("series_totales") val series: Int = 0,
    @SerialName("tonelaje_kg") val tonelaje: Double = 0.0,
    @SerialName("rpe_medio") val rpe: Double? = null,
    @SerialName("series_por_musculo") val porMusculo: Map<String, Double> = emptyMap(),
)

@Serializable
data class Volumen(val series: Double = 0.0, val objetivo: List<Double> = emptyList(), val estado: String = "ok")

@Serializable
data class E1rm(
    val ejercicio: String,
    @SerialName("series_8sem") val series: Int = 0,
    @SerialName("e1rm_ult_14d") val reciente: Double? = null,
    @SerialName("e1rm_dias_15_42") val previo: Double? = null,
    @SerialName("cambio_pct") val cambio: Double? = null,
    @SerialName("ultima_sesion") val ultima: Sesion? = null,
    @SerialName("mejor_por_semana") val porSemana: Map<String, Double> = emptyMap(),
)

@Serializable
data class Sesion(val fecha: String = "", val kg: Double? = null, val reps: Int? = null)

@Serializable
data class Carrera(
    val semanas: List<SemanaCarrera> = emptyList(),
    @SerialName("ultimas_2_semanas") val recientes: List<Salida> = emptyList(),
    @SerialName("dias_pista") val diasPista: DiasPista? = null,
)

/** Patrón de las sesiones del entrenador en la pista (últimas 8 semanas). */
@Serializable
data class DiasPista(
    val dias: List<String> = emptyList(),
    @SerialName("hora_media") val horaMedia: String? = null,
    @SerialName("sesiones_8_sem") val sesiones: Int = 0,
    val ultima: String? = null,
)

@Serializable
data class SemanaCarrera(
    val semana: String,
    val salidas: Int = 0,
    val km: Double = 0.0,
    @SerialName("ritmo_medio") val ritmo: String? = null,
    @SerialName("tirada_larga_km") val larga: Double = 0.0,
    @SerialName("fc_media") val fc: Double? = null,
)

@Serializable
data class Salida(
    val fecha: String,
    val nombre: String? = null,
    val km: Double = 0.0,
    val ritmo: String? = null,
    @SerialName("fc_media") val fc: Double? = null,
    val carga: Double? = null,
    @SerialName("te_aerobico") val te: Double? = null,
)

@Serializable
data class Carga(
    @SerialName("carga_garmin") val garmin: Ratio,
    @SerialName("km_carrera") val km: Ratio,
    @SerialName("series_fuerza") val series: Ratio,
)

@Serializable
data class Ratio(
    @SerialName("aguda_7d") val aguda: Double = 0.0,
    @SerialName("cronica_semanal_28d") val cronica: Double = 0.0,
    val ratio: Double? = null,
)

@Serializable
data class Recuperacion(
    val sueno: Sueno,
    val hrv: Hrv,
    @SerialName("fc_reposo") val fcReposo: Medias,
    @SerialName("body_battery_max_media_7d") val bodyBattery: Double? = null,
)

@Serializable
data class Sueno(
    @SerialName("horas_media_7d") val h7: Double? = null,
    @SerialName("horas_media_28d") val h28: Double? = null,
    @SerialName("noches_menos_7h_7d") val nochesCortas: Int = 0,
    @SerialName("puntuacion_media_7d") val pts7: Double? = null,
)

@Serializable
data class Hrv(
    @SerialName("media_7d") val m7: Double? = null,
    @SerialName("media_28d") val m28: Double? = null,
    @SerialName("rango_base") val base: List<Double?>? = null,
    @SerialName("estado_garmin") val estado: String? = null,
)

@Serializable
data class Medias(@SerialName("media_7d") val m7: Double? = null, @SerialName("media_28d") val m28: Double? = null)

@Serializable
data class Composicion(
    @SerialName("sin_datos") val sinDatos: Boolean = false,
    val ultimo: Pesaje? = null,
    @SerialName("peso_medio_7d") val peso7: Double? = null,
    @SerialName("kg_por_semana") val kgSemana: Double? = null,
    val inbody: Inbody? = null,
    @SerialName("ritmo_peso") val ritmo: RitmoPeso? = null,
)

@Serializable
data class Objetivo(val fase: String? = null)

@Serializable
data class Inbody(
    val ultimo: Medicion? = null,
    val anterior: Medicion? = null,
    @SerialName("mismo_equipo") val mismoEquipo: Boolean? = null,
    @SerialName("dias_entre") val diasEntre: Int? = null,
    /** Diferencias con la medición anterior: peso_kg, masa_muscular_kg, magro_piernas_kg… */
    val cambios: Map<String, Double?>? = null,
    val serie: List<Medicion> = emptyList(),
)

@Serializable
data class Medicion(
    val fecha: String = "",
    val equipo: String? = null,
    @SerialName("peso_kg") val peso: Double? = null,
    @SerialName("masa_muscular_kg") val musculo: Double? = null,
    @SerialName("masa_grasa_kg") val grasa: Double? = null,
    @SerialName("grasa_pct") val grasaPct: Double? = null,
)

/** Ritmo de peso frente al esperado para la fase (Garmin si hay pesajes; si no, InBody). */
@Serializable
data class RitmoPeso(
    @SerialName("kg_semana") val kgSemana: Double? = null,
    @SerialName("pct_semana") val pctSemana: Double? = null,
    val fuente: String? = null,
    val fase: String? = null,
    @SerialName("objetivo_pct_semana") val objetivo: List<Double> = emptyList(),
    @SerialName("en_rango") val enRango: Boolean? = null,
)

/** Plan de dieta del mes: kcal y macros ESTIMADAS a partir de las raciones (±15 %). */
@Serializable
data class Nutricion(
    val plan: String? = null,
    val estimado: Boolean = true,
    val tipos: Map<String, TipoDia> = emptyMap(),
    @SerialName("peso_ref") val pesoRef: PesoRef? = null,
    @SerialName("proteina_g_kg") val proteina: Map<String, Double?> = emptyMap(),
    @SerialName("carbohidratos_g_kg") val hidratos: Map<String, Double?> = emptyMap(),
    @SerialName("dias_7d") val dias: Map<String, Int> = emptyMap(),
    @SerialName("kcal_media_7d") val kcalMedia: Double? = null,
)

@Serializable
data class TipoDia(
    val kcal: Double? = null,
    @SerialName("proteina_g") val proteina: Double? = null,
    @SerialName("carbohidratos_g") val hidratos: Double? = null,
    @SerialName("grasa_g") val grasa: Double? = null,
)

@Serializable
data class PesoRef(val kg: Double? = null, val fecha: String? = null, val fuente: String? = null)

@Serializable
data class Pesaje(
    val fecha: String = "",
    @SerialName("peso_kg") val peso: Double? = null,
    @SerialName("grasa_pct") val grasa: Double? = null,
)

@Serializable
data class Dia(
    val d: String,
    @SerialName("sueno_h") val sueno: Double? = null,
    val hrv: Double? = null,
    @SerialName("hrv_lo") val hrvLo: Double? = null,
    @SerialName("hrv_hi") val hrvHi: Double? = null,
    val fcr: Double? = null,
    val bb: Double? = null,
    val peso: Double? = null,
    val km: Double? = null,
    val gym: Int? = null,
)

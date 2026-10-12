# AEGrief

**AEGrief** es un sistema avanzado de reclamos, delimitación y protección de terrenos contra griefing para servidor de Minecraft Spigot / Paper / Purpur, con compatibilidad híbrida para clientes Java y Bedrock.

---

## 🚀 Características Principales

- **Sistema de Flags Avanzadas:**
  - `ENTRY`: Campo de fuerza dinámico perimetral con aislamiento de intrusos y auto-expulsión fuera del área delimitada.
- **Seguridad Rigurosa de Reclamos:**
  - Validación estricta de propiedad en `/transferclaim` para prevenir cesiones no autorizadas de terrenos.
- **Interfaz Gráfica Adaptada (Cross-Platform):**
  - Sistema de confirmación visual e interactiva (`ProtectionConfirmGui`) con opciones táctiles (Lana Verde/Roja) diseñada tanto para clientes de Java como de Bedrock.
- **Rendimiento y Persistencia:**
  - Optimización en la comprobación periódica de límites y guardado asíncrono de datos de reclamos.

---

## 🛡️ Registro de Errores Resueltos y Auditoría de Seguridad

Para consultar el registro técnico detallado de vulnerabilidades neutralizadas, exploits de duplicación prevenidos y correcciones de compatibilidad multiplataforma, consulta:
👉 [**Registro de Errores Resueltos y Seguridad (SECURITY_AND_FIXES.md)**](SECURITY_AND_FIXES.md)

---

## 🛠️ Requisitos de Compilación

- **Java JDK:** 17 o superior.
- **Maven:** 3.8+

---

## 📦 Compilación

Para compilar el proyecto y generar el archivo `.jar`:

```bash
mvn clean package -DskipTests
```

El binario compilado se generará en la carpeta `target/`.

---

## 📄 Licencia

Este proyecto está bajo la licencia [GPLv3](LICENSE).

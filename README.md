# Sistema de Gestión de Citas Médicas (TF_EquipoNN)

Proyecto final del curso de Programación Orientada a Objetos (Java 21, consola, SQL Server).

## Cómo abrirlo en IntelliJ IDEA

1. Descomprime el ZIP.
2. `File > Open...` y elige el archivo **`pom.xml`** (o la carpeta `TF_EquipoNN`) → **Open as Project** → **Trust Project**.
3. Espera a que Maven descargue las dependencias (barra de progreso abajo a la derecha).
4. Comprueba el JDK: `File > Project Structure > Project > SDK = 21` (si no aparece, `Add SDK > Download JDK > 21`).
5. Abre `src/main/java/app/Main.java` y pulsa el triángulo verde ▶ junto a `main`.

> Si cambias el nombre del proyecto, reemplaza `NN` en `TF_EquipoNN` por el número de tu equipo
> (carpeta y `artifactId` del `pom.xml`).

## Cómo funciona

El sistema es de **recepción de una posta médica**: la recepcionista programa y cobra citas. El personal ya viene
cargado, por eso **no se registran** médicos, recepcionistas ni consultorios, y **tampoco recetas** (las hace el médico, no recepción):

- 2 recepcionistas (al iniciar se elige quién atiende, escribiendo su número).
- 5 médicos (Medicina General, Odontología, Enfermería, Obstetricia, Emergencias) con CMP y RNE.
- 5 consultorios (101, 102, 103 en el piso 1; 201, 202 en el piso 2).

Todo se elige **escribiendo el número** de la lista que aparece en pantalla (0 = cancelar). Programar una cita
(opción 2) es: DNI del paciente (si no existe se registra ahí mismo) → número de médico → número de día →
número de turno → número de consultorio libre. Solo se ofrecen horas realmente libres: el médico atiende de
lunes a sábado, de 08:00 a 18:00, en turnos de 30 minutos.

## Regla de cobro

| Seguro | ¿Paga? |
|---|---|
| SIS Gratuito, SIS Emprendedor (NRUS), SIS Microempresas | No |
| SIS Independiente | No, si está al día en sus aportes (se pregunta al registrarlo) |
| EsSalud, EPS / privado, FF.AA. / PNP, sin seguro | Sí: tarifa de la especialidad |

Flujo de una cita: **Programada → pago en caja (opción 3, si corresponde) → Confirmada → Atendida**.
Al pagar, la cita queda confirmada sola; a los pacientes exonerados se les confirma con la opción 4.
Los ingresos de los reportes solo cuentan las citas cobradas.

## Base de datos SQL Server (opción 11 del menú)

1. En **SQL Server Management Studio (SSMS)** abre `sql/schema.sql` y ejecútalo completo con F5. Crea la base
   `citas_medicas`, sus tablas **y carga el personal predeterminado**. Se puede ejecutar varias veces sin problema
   (también actualiza una base creada con la versión anterior).
2. Habilita el acceso por **TCP/IP** (SQL Server Configuration Manager → Protocols for MSSQLSERVER/SQLEXPRESS → TCP/IP → Enabled,
   y reinicia el servicio) y usa **autenticación mixta** con un usuario SQL (por ejemplo `sa`) y su clave.
   El driver JDBC no usa tu usuario de Windows por defecto.
3. Por defecto se conecta a `localhost:1433`, base `citas_medicas`, usuario `sa`. Define estas variables de entorno en
   `Run > Edit Configurations > Environment variables` (separadas por `;`):
   - `DB_USER=sa`
   - `DB_PASSWORD=tu_clave`
   - `DB_URL` (opcional) por ejemplo, para SQL Server Express con instancia con nombre:
     `jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;databaseName=citas_medicas;encrypt=true;trustServerCertificate=true`
4. **Al iniciar**, el programa lee de la base el personal, los pacientes y las citas guardadas. Si no hay conexión,
   usa el mismo personal desde `app/PersonalPredeterminado.java` y avisa en pantalla: el sistema funciona igual.
5. En el menú: **11 → 1** prueba la conexión y **11 → 2** guarda las citas (con sus pacientes).

## Primer recorrido sugerido

`12` (pacientes y citas de ejemplo) → `6` (listar citas) → `3` (cobrar la cita de Luis, que tiene EsSalud) →
`4` (confirmar la cita de Ana, con SIS, y luego atenderla) → `2` (programar una cita nueva eligiendo todo por número) →
`5` (comprobante en HTML) → `7` (médicos, consultorios y reporte de disponibilidad) →
`9` (reportes y exportar HTML/XML) → `10` (guardar archivo) → `11` (SQL Server).

Los reportes y comprobantes se generan en la carpeta `reportes_generados/` y la serialización en
`datos_centro.dat` (ambos dentro de la carpeta del proyecto). Si ya tenías un `datos_centro.dat` de una
versión anterior, bórralo: el modelo cambió y no se podrá leer.

## Estructura

| Paquete | Clases |
|---|---|
| `modelo` | `Persona` (sellada y abstracta), `Paciente`, `Medico`, `Recepcionista`, `CentroSalud`, `Consultorio`, `Cita`, `HistoriaClinica` + `EstadoCita`, `Especialidad`, `TipoSeguro`, `CitaResumen` (record) |
| `pagos` | `MetodoPago` (interfaz), `PagoTarjeta`, `PagoEfectivo` |
| `reportes` | `Reporte` (abstracta, método plantilla), `ReporteHTML`, `ReporteXML`, `ComprobanteCita`, `ReporteDisponibilidad` |
| `util` | `Paginador<T>` (genérica), `LectorConsola`, `Consola`, `Navegador` |
| `dao` | `DAO<T>` (interfaz genérica), `ConexionBD`, `CitaDAO`, `CentroDAO` (carga el personal al iniciar) |
| `app` | `Main`, `MenuPrincipal`, `PersonalPredeterminado` |

## Dónde se aplica cada tema del sílabo

| Tema | Dónde está |
|---|---|
| Encapsulamiento, constructores y sobrecarga | Atributos privados; `Cita` y `Paciente` tienen dos constructores |
| Lectura de consola con validaciones y excepciones | `LectorConsola` |
| Herencia, miembros `protected`, sealed classes | `Persona` → `Paciente`, `Medico`, `Recepcionista` |
| Enumeraciones con atributos | `Especialidad` (tarifa), `TipoSeguro` (regla de cobro), `EstadoCita` |
| Clases abstractas y polimorfismo | `Persona.getRol()`, `Reporte` y sus hijas |
| Método plantilla (HTML/XML) | `Reporte.generar()` |
| Interfaces | `MetodoPago`, `DAO<T>` |
| Asociación, agregación, composición, dependencia | Ver diagrama UML |
| Colecciones (`List`, `Map`), CRUD con `HashMap`/`ArrayList` | `CentroSalud` (`Map` de pacientes, `List` de citas) |
| Sequenced collections | `HistoriaClinica.ultimasAtenciones()` |
| Clases genéricas | `Paginador<T>`, `DAO<T>`, `MenuPrincipal.elegirDeLista` (método genérico) |
| Record classes | `CitaResumen` |
| Base de datos (JDBC, CRUD) | `ConexionBD`, `CitaDAO`, `sql/schema.sql` |
| Serialización | `CentroSalud.guardarEn()` / `cargarDe()` |
| Lambdas y streams | Consultas y reportes de `CentroSalud` |

## Pendiente (para los siguientes avances)

- DAO de `Paciente` (hoy `CitaDAO` guarda al paciente solo cuando tiene una cita).
- Lectura y escritura en archivos de texto/CSV.

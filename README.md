#### Enlace a Figma https://www.figma.com/design/EoLxx02ZO1RgonULxe9xRh/KeePet_Laboratorio1?node-id=0-1&t=g7g9xGoERp8Huao3-1

# KeePet - Manual de Usuario

> 🐾 **Plataforma móvil para la gestión, control y administración del cuidado integral de mascotas.*

---

##  Tabla de Contenidos

- [Requisitos del Sistema](#-requisitos-del-sistema)  
- [Descarga e Instalación](#%EF%B8%8F-descarga-e-instalaci%C3%B3n)
- [Flujo de Autenticación](#-flujo-de-autenticaci%C3%B3n)
- [Módulo de Cliente (Dueño de Mascota)](#-m%C3%B3dulo-de-cliente-due%C3%B1o-de-mascota)
  - 1️ [Registro y Creación de Expediente](#1%EF%B8%8F%E2%83%A3-registro-y-creaci%C3%B3n-de-expediente-en-3-pasos)
  - 2️ [Pantalla de Inicio y Filtros](#2%EF%B8%8F%E2%83%A3-pantalla-de-inicio-y-b%C3%BAsqueda)
  - 3️ [Agendamiento de Citas](#3%EF%B8%8F%E2%83%A3-agendamiento-de-citas)
  - 4️ [Gestión de Citas y Código QR](#4%EF%B8%8F%E2%83%A3-gesti%C3%B3n-de-citas-y-c%C3%B3digo-qr)
  - 5️ [Expediente e Historial Médico](#5%EF%B8%8F%E2%83%A3-expediente-m%C3%A9dico-e-historial)
  - 6️ [Perfil de Usuario y Configuración](#6%EF%B8%8F%E2%83%A3-perfil-de-usuario-y-configuraci%C3%B3n)
-  [Módulo de Médico y Empleado (Personal de Clínica)](#%EF%B8%8F-m%C3%B3dulo-de-m%C3%A9dico-y-empleado-personal-de-cl%C3%ADnica)
  - 1️ [Dashboard de Actividades del Día](#1%EF%B8%8F%E2%83%A3-dashboard-de-actividades-del-d%C3%ADa)
  - 2️ [Agenda de Pacientes](#2%EF%B8%8F%E2%83%A3-agenda-de-pacientes)
  - 3️ [Gestión y Creación de Expediente Clínico](#3%EF%B8%8F%E2%83%A3-gesti%C3%B3n-y-creaci%C3%B3n-de-expediente-cl%C3%ADnico)
  - 4️ [Agendamiento Interno de Citas](#4%EF%B8%8F%E2%83%A3-agendamiento-interno-de-citas)
- [Módulo de Administración](#%EF%B8%8F-m%C3%B3dulo-de-administrac%C3%B3n)
  - 1️ [Dashboard de Actividades y Recepción Global](#1%EF%B8%8F%E2%83%A3-dashboard-de-actividades-y-recepci%C3%B3n-global)
  - 2️ [Agenda y Control de Pacientes](#2%EF%B8%8F%E2%83%A3-agenda-y-control-de-pacientes)
  - 3️ [Expedientes e Historiales Clínicos](#3%EF%B8%8F%E2%83%A3-expedientes-e-historiales-cl%C3%ADnicos)
  - 4️ [Gestión de Usuarios y Roles del Sistema](#4%EF%B8%8F%E2%83%A3-gesti%C3%B3n-de-usuarios-y-roles-del-sistema-exclusivo-administrador)
  - 5️ [Agendamiento Directo de Citas](#5%EF%B8%8F%E2%83%A3-agendamiento-directo-de-citas)

---

##Requisitos del Sistema

| Requisito | Especificación Mínima |
| :--- | :--- |
| **Sistema Operativo** | Android 8.0 (Oreo) o superior / iOS 12.0 o superior |
| **Conectividad** | Conexión a Internet activa (Wi-Fi o Datos Móviles 3G/4G/5G) |
| **Almacenamiento** | 50 MB de espacio disponible |
| **Componentes Adicionales** | Cámara para escaneo de códigos QR |

---

## Descarga e Instalación

1. **Descargar la App:** Descargue el archivo de la aplicación desde el repositorio oficial o tienda de aplicaciones correspondiente.
2.  **Permisos de Instalación:** Al abrir el instalador por primera vez en Android, habilite la opción *"Permitir la instalación de aplicaciones de fuentes desconocidas"* en los ajustes de su navegador o gestor de archivos.
3.  **Completar Instalación:** Presione **Instalar**, espere la confirmación del sistema e inicie **KeePet** desde el menú principal de su dispositivo.

---

## Flujo de Autenticación

### 1️ Pantalla de Bienvenida
-  Al ingresar por primera vez, se presenta la pantalla inicial con el lema *"Cuidamos a los que más amas"*.
-  Presione el botón **Siguiente →** para acceder a la pantalla de inicio de sesión.

### 2️ Inicio de Sesión
-  **Ingreso de credenciales:** Introduzca su **Usuario / Correo Electrónico** y su **Contraseña**.
-  **Selección de Rol / Tipo de Usuario:**
  -  **Administrador:** Acceso total a la gestión de actividades, agenda, expedientes y control de usuarios.
  -  **Doctor / Empleado:** Acceso al panel de atención médica, recepción, agenda y expediente clínico.
  -  **Usuario:** Acceso directo para dueños de mascotas.

---

##  Módulo de Cliente (Dueño de Mascota)

El módulo de cliente permite a los propietarios registrar a sus mascotas, programar citas de salud y estética, consultar expedientes médicos e interactuar fácilmente con la clínica.

### 1️ Registro y Creación de Expediente (en 3 Pasos)
*  **Paso 1 - Selección de Especie:** Seleccione la categoría de su mascota (**Perro**, **Gato** o **Conejo**) y presione **Siguiente →**.
*  **Paso 2 - Datos Generales:** Ingrese **Nombre**, **Edad** y **Raza** de la mascota, junto con los datos del dueño (**Nombre Completo**, **Teléfono** y **Dirección**). Presione **Siguiente →**.
*  **Paso 3 - Detalles Médicos Adicionales:** Indique **Alergias conocidas**, **Condiciones médicas** preexistentes y **Vacunas** aplicadas previamente. Presione **Finalizar / Guardar**.

### 2️ Pantalla de Inicio y Búsqueda
-  **Buscador de Mascotas:** Barra superior *"Buscar mascota por nombre"*.
-  **Filtros por Categoría:** Botones rápidos para filtrar por **Todos**, **Perros**, **Gatos** o **Conejos**.
-  **Tarjetas de Mascota:** Muestra la fotografía, nombre, raza, edad, dueño registrado y opciones directas de gestión.
-  **Barra de Navegación Inferior:**
  -  **Inicio:** Vista principal del listado de mascotas.
  -  **Citas:** Historial y gestión de citas programadas.
  -  **Agendar:** Acceso directo para programar una nueva cita.
  -  **Perfil:** Configuración de la cuenta de usuario.

### 3️ Agendamiento de Citas
1.  **Seleccionar Mascota:** Elija la mascota que recibirá la atención (*Milo, Copito, Oliver, etc.*) o seleccione **+ Añadir** para registrar una nueva.
2.  **Seleccionar Servicio:** Elija entre las opciones disponibles con tarifa transparente:
   -  **Consulta General** (*$20.00*)
   -  **Vacunación** (*$15.00*)
   -  **Desparasitación**
   -  **Control y Seguimiento**
   -  **Valoración Quirúrgica**
   -  **Urgencia** (*$25.00*)
   -  **Baño y Estética** (*$10.00*)
3.  **Seleccionar Fecha y Hora:** Marque el día deseado en el calendario e indique el horario disponible.
      **Resumen de Cita:** Confirme los detalles del servicio, precio e ingrese **Notas adicionales** para el personal si es necesario. Presione **Confirmar →**.
5.  **Confirmación:** La pantalla *"¡Todo listo!"* valida que la cita ha sido agendada con éxito.

### 4️ Gestión de Citas y Código QR
-  **Detalles de la Cita:** Visualice la fotografía de la mascota, tipo de servicio, fecha y hora asignada.
-  **Mostrar QR de la Cita:** Genera un código QR único que se presenta en la recepción de la clínica para agilizar el ingreso.
-  **Cancelar Cita:** Permite anular la reserva de manera anticipada.

### 5️ Expediente Médico e Historial
-  **Perfil de la Mascota:** Fotografía, datos básicos y botón de acceso rápido **Agendar cita**.
-  **Datos Generales:** Especie, raza, edad, peso (*ej. 8.2 kg*) y sexo.
-  **Datos del Dueño:** Nombre del propietario, número de teléfono, dirección y correo electrónico.
-  **Información Médica Destacada:** Alertas de alergias e indicaciones especiales (*ej. temperamento o cuidados al secar*).
-  **Historial Clínico Cronológico:** Baño Estético, Vacunación Anual, Consulta General, Corte de pelo y arreglo de uñas.

### 6️ Perfil de Usuario y Configuración
-  **Mis Datos:** Nombre completo (*ej. Anya Forge*), teléfono, dirección y correo con opción de edición.
-  **Configuración y Seguridad:**
  -  **Configuración de la Cuenta:** Notificaciones y preferencias.
  -  **Seguridad:** Cambio de contraseña y protección de datos.
  -  **Centro de Ayuda:** Preguntas frecuentes y canales de soporte técnico.
  -  **Cerrar Sesión:** Salida segura de la aplicación.

---

##  Módulo de Médico y Empleado (Personal de Clínica)

El personal clínico comparte un panel unificado diseñado para optimizar el flujo de atención, la recepción de pacientes y el control clínico.

### 1️ Dashboard de Actividades del Día
-  **Buscador General:** Permite buscar rápidamente por nombre de mascota, cliente o código de cita en la barra *"Buscar mascota, cliente y código..."*.
-  **Filtros de Estado:** Clasificación de citas en pestañas: **Activas**, **Hoy**, **Completadas** y **Canceladas**.
-  **Registrar Ingreso:** Botón para confirmar manualmente el arribo del paciente a las instalaciones.
-  **Escanear QR:** Opción para activar la cámara del dispositivo y escanear el código QR del cliente, validando su cita al instante.

### 2️ Agenda de Pacientes
-  **Control Cronológico:** Muestra el listado diario de mascotas citadas con hora, tipo de servicio y doctor asignado.
-  **Visualización Rápida:** Acceso a tarjetas individuales con foto del paciente, especie, nombre del dueño e icono de enlace directo al expediente.
-  **Barra de Navegación Inferior:** Acceso directo entre **Actividades** y **Agenda**

### 3️ Gestión y Creación de Expediente Clínico
-  **Creación de Expediente:** Flujo en 3 pasos (*Especie, Datos Generales y Antecedentes Médicos*).
-  **Edición de Datos:** Iconos de edición directa para actualizar el peso (*ej. 8.2 kg*), dirección, teléfono o datos de la mascota.
-  **Añadir Registro Médico:** Botón para adjuntar diagnósticos, recetas, notas de baño o vacunación directamente al expediente de la mascota.
-  **Historial Clínico Unificado:** Registro cronológico de consultas, vacunaciones antirrábicas, desparasitaciones y sesiones estéticas.

### 4️ Agendamiento Interno de Citas
-  **Reserva Presencial / Telefónica:** Módulo para programar citas de clientes que se encuentren físicamente en la clínica o llamen por teléfono.
-  **Selección de Servicios y Horarios:** Selección de servicio, fecha en el calendario e indicación de horario disponible.
-  **Confirmación de Cita:** Emisión de comprobante resumen (*"¡Todo listo!"*).

---

##  Módulo de Administración

El administrador cuenta con acceso total al sistema, supervisando la recepción, agenda, expedientes y una sección exclusiva para el control del personal y usuarios del sistema.

### 1️ Dashboard de Actividades y Recepción Global
-  **Monitoreo de Citas:** Control global de todas las citas del establecimiento clasificadas por estado (**Activas**, **Hoy**, **Completadas** y **Canceladas**).
-  **Validación de Ingreso y Cancelación:** Botón para marcar el ingreso del paciente o cancelar citas que no puedan procesarse.
-  **Escanear QR:** Herramienta de lectura rápida para agilizar el registro de entrada de las mascotas.

### 2️ Agenda y Control de Pacientes
-  **Filtro y Búsqueda de Citas:** Búsqueda rápida por nombre de paciente o dueño.
-  **Listado General:** Vista consolidada de todos los servicios programados para la jornada clínica.

### 3️ Expedientes e Historiales Clínicos
-  **Control de Fichas Clínicas:** Supervisión completa de los expedientes de todas las mascotas registradas.
-  **Modificación de Información:** Facultad de editar datos de contacto de los dueños, peso, alergias o agregar registros médicos directos.

### 4️ Gestión de Usuarios y Roles del Sistema (Exclusivo Administrador)
-  **Menú Inferior Exclusivo ("Usuarios"):** Pestaña dedicada a la administración de cuentas registradas en KeePet.
-  **Asignación y Cambio de Roles:** Modificación de los permisos de los usuarios del sistema (*Administrador, Doctor, Empleado, Usuario*).
-  **Eliminación de Cuentas:** Opción directa para dar de baja o eliminar usuarios y personal de la base de datos.
-  **Escanear QR de Usuario:** Opción para vincular cuentas o verificar credenciales mediante escaneo de código QR.

### 5️ Agendamiento Directo de Citas
-  **Módulo de Reserva Administrativa:** Herramienta para agendar citas directamente desde recepción o administración.
-  **Confirmación e Historial:** Emisión de resúmenes de citas agendadas con detalle de tarifa, hora y observaciones especiales.

---
*KeePet © 2026 - Todos los derechos reservados.

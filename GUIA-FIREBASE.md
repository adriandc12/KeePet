# Conectar KeePet con Firebase — paso a paso

El código ya está listo. Falta lo único que yo no puedo hacer por ti: crear tu
proyecto en Firebase y descargar tu archivo de credenciales.

Tardarás unos 20 minutos. Ve en orden: cada paso da por hecho el anterior.

Son dos servicios distintos y conviene tener claro qué hace cada uno:

- **Firebase** (pasos 1 a 6, y 8) → el login y los datos: usuarios, mascotas,
  citas, roles, historial médico.
- **Cloudinary** (paso 7) → solo los archivos de imagen. En Firebase se guarda
  únicamente la URL de cada foto.

---

## Paso 1 — Crear el proyecto en Firebase

1. Entra en <https://console.firebase.google.com> con tu cuenta de Google.
2. Pulsa **Crear un proyecto** (o *Add project*).
3. Nombre: `KeePet`. Pulsa **Continuar**.
4. En la pantalla de Google Analytics puedes **desactivarlo**: no lo necesitas y
   te ahorra pasos. Pulsa **Crear proyecto** y espera.

---

## Paso 2 — Registrar la app de Android

1. Ya dentro del proyecto, pulsa el icono de **Android** (⊕ Android).
2. En **Nombre del paquete de Android** escribe exactamente:

   ```
   com.example.keepet
   ```

   ⚠️ Tiene que coincidir carácter por carácter con el `applicationId` de
   `app/build.gradle.kts`. Si no coincide, la app compila pero Firebase la
   rechaza al arrancar.

3. El apodo y el SHA-1 puedes dejarlos vacíos. Pulsa **Registrar app**.

---

## Paso 3 — Descargar y colocar `google-services.json`

1. Firebase te ofrecerá descargar **`google-services.json`**. Descárgalo.
2. En tu proyecto ya hay un archivo con ese nombre en:

   ```
   KeePet/app/google-services.json
   ```

   Ese es un **marcador de posición que puse yo para que el proyecto compilara**.
   Ábrelo y verás valores como `REEMPLAZAME` y `reemplazame-keepet`.

3. **Bórralo y pon el tuyo en su lugar**, en esa misma carpeta `app/`.

> Con el archivo falso la app compila pero **no se conecta**: al intentar entrar
> dará un error de autenticación. Es la señal de que este paso falta.

4. En la consola de Firebase pulsa **Siguiente** hasta salir del asistente
   (los pasos de Gradle que te muestra ya están hechos en tu proyecto).

---

## Paso 4 — Activar el inicio de sesión con correo

1. Menú izquierdo → **Compilación** → **Authentication**.
2. Pulsa **Comenzar**.
3. Pestaña **Sign-in method** → elige **Correo electrónico/contraseña**.
4. Activa el **primer interruptor** (*Habilitar*). El segundo (*vínculo por
   correo*) déjalo apagado.
5. **Guardar**.

Sin este paso, registrarse falla con `CONFIGURATION_NOT_FOUND`.

---

## Paso 5 — Crear la Realtime Database

⚠️ Cuidado: en el menú hay **dos** bases de datos distintas. Necesitas
**Realtime Database**, NO *Firestore Database*. Son productos diferentes y el
código usa la primera.

1. Menú izquierdo → **Compilación** → **Realtime Database**.
2. Pulsa **Crear base de datos**.
3. Ubicación: la que te ofrezca por defecto sirve.
4. Elige **Comenzar en modo bloqueado**. Las reglas buenas las pones en el paso 6.
5. **Habilitar**.

---

## Paso 6 — Poner las reglas de la base de datos

1. Dentro de Realtime Database, pestaña **Reglas**.
2. Borra lo que haya.
3. Abre el archivo **`firebase-database-rules.json`** (está en la raíz del
   proyecto, junto a `gradlew`), copia **todo** su contenido y pégalo ahí.
4. Pulsa **Publicar**.

No lo copio aquí dentro para que no haya dos versiones que puedan acabar
distintas: la que manda es la del archivo.

**Qué hacen esas reglas, en cristiano:**

- `auth != null` → si no has iniciado sesión, no lees ni escribes nada.
- `auth.uid === $uid` → solo puedes tocar la rama que lleva **tu**
  identificador. Aunque alguien supiera el `uid` de otra persona, el servidor le
  diría que no. La seguridad está en el servidor, no en la app: eso es lo
  importante, porque una app se puede modificar y el servidor no.
- **Los roles viven en su propia rama `/roles`, separada de `/usuarios`.**
  Esto es a propósito y es la decisión de seguridad más importante de todo el
  diseño: tú puedes editar tu perfil (tu nombre, tu foto), pero **no puedes
  editar tu rol**. Si el rol estuviera dentro de tu perfil, cualquier cliente
  podría ascenderse a administrador. Ahí solo escribe un admin.
- La única excepción: al registrarte, tu propia app crea tu nodo de rol **una
  vez** y **solo con el valor `cliente`** (`!data.exists() && newData.val() ==
  'cliente'`). Una vez creado, ya no puedes cambiarlo tú.
- El personal (empleado, doctor, admin) puede leer `/usuarios` completo, porque
  necesita ver los pacientes de todos los clientes. Un cliente solo se ve a sí
  mismo.
- `historial` y `notasMedicas` de una mascota los puede escribir el dueño **o**
  un doctor/admin. Así el veterinario apunta el diagnóstico en la ficha de una
  mascota que no es suya.
- **Los expedientes (`/usuarios/{uid}/mascotas`) los puede crear, editar y borrar
  todo el personal**, no solo el dueño. Es lo que hace funcionar la pestaña
  *Expedientes*, que antes estaba en la app del cliente y ahora está en la de la
  clínica. Ten claro lo que concede: cualquier empleado puede borrar la mascota
  de un cliente. Si algún día quieres limitarlo a doctores y admin, en el bloque
  `mascotas` cambia `!= 'cliente'` por `== 'doctor' || == 'admin'`. El dueño
  sigue pudiendo editar las suyas por la regla general de su propia rama.
- `/citas` es una rama global (todas las citas de la clínica juntas), con
  `.indexOn` para que las búsquedas sean rápidas. Un cliente solo puede
  consultarla **filtrando por su propio uid** (eso es lo que comprueba
  `query.orderByChild == 'clienteUid' && query.equalTo == auth.uid`); el
  personal la puede leer entera.

---

## Paso 7 — Configurar Cloudinary (para las fotos)

Las fotos **no** se guardan en la base de datos. La base de datos está hecha
para texto y números; una foto son cientos de miles de bytes. Hace falta un
"disco duro en la nube" aparte. En la base de datos solo se guarda la **URL** de
la foto, que es una línea de texto.

> **¿Por qué Cloudinary y no Firebase Storage?** Firebase Storage pasó a exigir
> el plan de pago **Blaze** (con tarjeta de crédito) incluso para usarlo dentro
> del límite gratuito. Cloudinary hace lo mismo, tiene 25 GB gratis y **no pide
> tarjeta**. Todo lo demás sigue en Firebase: la base de datos y el login no
> cambian. Lo único que cambió es dónde viven los archivos de imagen.

### 7.1 — Crear la cuenta

1. Entra en <https://cloudinary.com> y pulsa **Sign up for free**.
2. Regístrate con tu correo. No pide tarjeta.
3. Cuando entres, verás el **Dashboard**.

### 7.2 — Copiar el `cloud_name`

En el Dashboard, arriba, verás un bloque con **Cloud name**. Es una palabra
corta, tipo `dxy3abc9z`. Cópiala.

⚠️ No es tu correo ni tu nombre de usuario. Es ese identificador corto.

### 7.3 — Crear el *upload preset*

Esto es lo que permite que la app suba fotos **sin llevar dentro ninguna
contraseña**. Se llama *subida sin firmar* (unsigned).

1. Pulsa la **rueda dentada** (Settings) arriba a la derecha.
2. Menú **Upload** → baja hasta **Upload presets**.
3. Pulsa **Add upload preset**.
4. En **Upload preset name** escribe:

   ```
   keepet_unsigned
   ```

5. En **Signing mode** cambia `Signed` por **`Unsigned`**. ⚠️ **Este es el paso
   que de verdad importa.** Si lo dejas en `Signed`, la app dará error al subir,
   porque una app móvil no puede firmar sin llevar la clave secreta dentro.
6. Pulsa **Save**.

### 7.4 — Poner los dos datos en el proyecto

1. En Android Studio abre el archivo **`local.properties`** (está en la raíz del
   proyecto, junto a `gradlew`).
2. Al final verás estas dos líneas, que ya dejé preparadas:

   ```properties
   cloudinary.cloudName=REEMPLAZAME
   cloudinary.uploadPreset=REEMPLAZAME
   ```

3. Sustituye los dos `REEMPLAZAME`. Por ejemplo:

   ```properties
   cloudinary.cloudName=dxy3abc9z
   cloudinary.uploadPreset=keepet_unsigned
   ```

4. **File → Sync Project with Gradle Files** y vuelve a ejecutar la app.

### ¿Por qué en `local.properties` y no en el código?

Por dos razones:

- **`local.properties` nunca se sube a GitHub.** Viene ignorado de serie. Si
  pusiera tu `cloud_name` dentro de un archivo `.kt`, acabaría publicado el día
  que subas el proyecto.
- **Para cambiarlo no hace falta tocar código.** Si mañana usas otra cuenta,
  cambias una línea de texto y listo.

### Sobre seguridad, para que lo entiendas bien

En tu cuenta de Cloudinary hay tres datos: `cloud_name`, `api_key` y
`api_secret`. **En la app solo va el `cloud_name` y el nombre del preset.** El
`api_secret` **no aparece en ningún sitio del proyecto**, y eso es a propósito:
un APK se puede descompilar en dos minutos, así que cualquier secreto que metas
dentro deja de ser secreto en cuanto publicas la app.

Lo peor que puede hacer alguien que saque el preset de tu APK es subir imágenes
a tu cuenta. No puede borrar nada, ni leer tu cuenta, ni cambiar la
configuración.

El precio de esta decisión: **la app no puede borrar fotos de Cloudinary**
(borrar sí requiere el secreto). Cuando borras una mascota, su foto se queda
ahí ocupando espacio. Con 25 GB gratis no es un problema; si algún día molesta,
se borran a mano desde **Media Library** en el panel de Cloudinary.

Y una consecuencia más: cada vez que cambias la foto de una mascota se crea un
archivo nuevo en lugar de reemplazar el viejo. Las subidas sin firmar no pueden
sobrescribir. La app siempre muestra la última, que es lo que importa.

---

## Paso 8 — Crear el primer administrador (a mano)

Aquí hay un problema del huevo y la gallina: **solo un admin puede nombrar
admins**, y al principio no hay ninguno. Así que el primero se pone a mano
desde la consola. Se hace una sola vez.

1. Registra tu cuenta desde la app (Paso 9). Tienes que hacerlo **antes**,
   porque necesitas tu `uid`, y el `uid` no existe hasta que la cuenta existe.
2. Consola de Firebase → **Authentication** → pestaña **Users**.
3. Busca tu correo y copia su **User UID** (una cadena larga tipo
   `k3Jd8sLp2...`). Hay un botón para copiarlo.
4. Ve a **Realtime Database** → pestaña **Datos**.
5. Sitúate en el nodo **`roles`** (si ya te registraste, existe y dentro está tu
   uid con el valor `cliente`).
6. Pulsa el **lápiz** sobre tu uid y cambia el valor `cliente` por:

   ```
   admin
   ```

7. Pulsa Enter para guardar.
8. En la app: **cierra sesión y vuelve a entrar**. Ahora verás las pantallas del
   personal (Agenda, Pacientes, Usuarios) en lugar de las de cliente.

Los valores válidos son exactamente estos cuatro, en minúscula:

| Valor | Quién es | Qué puede hacer |
|---|---|---|
| `cliente` | El dueño de la mascota | Sus mascotas, sus citas, su QR |
| `empleado` | Recepción | Ver la agenda, confirmar/cancelar citas, escanear QR, asignar doctor |
| `doctor` | Veterinario | Todo lo del empleado **+ escribir en el historial médico** |
| `admin` | Tú | Todo **+ cambiar el rol de los demás** |

> Si escribes cualquier otra cosa (`Admin`, `administrador`, `doctora`…), la
> regla de seguridad lo rechaza y la app te trata como `cliente`. Es a
> propósito: es mejor que un error tipográfico te deje sin permisos que que te
> los dé de más.

Una vez tienes un admin, **ya no hace falta volver a la consola nunca**: desde
la app, en la pestaña **Usuarios**, el admin cambia los roles de los empleados
y doctores tocando un botón.

---

## Paso 9 — Probar

1. En Android Studio: **File → Sync Project with Gradle Files**.
2. Ejecuta la app (▶).
3. Verás la bienvenida → **Login**. Pulsa **"No tengo cuenta, registrarme"**.
4. Escribe un correo y una contraseña de **6 caracteres o más**.
5. Pulsa **Registrarme**.

Deberías entrar como **cliente** y ver tres mascotas de ejemplo (Milo, Oliver,
Copito).

**Para comprobar que de verdad está en la nube:** vuelve a la consola de
Firebase → Realtime Database → pestaña **Datos**. Verás el árbol creándose:

```
roles
  └── AbC123xyz...  : "cliente"      <- tu rol, en su propia rama

usuarios
  └── AbC123xyz...                   <- tu uid
       ├── perfil
       └── mascotas
            └── -Nx1a...             <- clave que genera Firebase sola
                 ├── nombre : "Milo"
                 ├── fotoUrl : "https://firebasestorage..."
                 └── historial

citas                                <- rama global, no dentro de tu usuario
  └── -Ny7b...
       ├── clienteUid : "AbC123xyz..."
       ├── mascotaId  : "-Nx1a..."
       ├── estado     : "pendiente"
       └── doctorUid  : ""
```

Añade una mascota desde el móvil y mira cómo aparece ahí **al instante**.

**Por qué las citas están fuera de `usuarios`:** el personal de la clínica
necesita ver la agenda del día completa, de todos los clientes a la vez. Si cada
cita viviera dentro de su dueño, para armar la agenda habría que recorrer todos
los usuarios uno por uno. Con una rama global es una sola consulta.

### Prueba las funciones nuevas

1. **Foto de mascota** — entra en una mascota → botón de la cámara → haz la foto.
   Compruébalo en el panel de **Cloudinary** → **Media Library** → carpeta
   `keepet/mascotas`. Y en Realtime Database verás que el campo `imagenUri` de
   esa mascota ahora contiene una URL que empieza por
   `https://res.cloudinary.com/...`.
2. **QR de la cita** — crea una cita y pulsa el botón de QR en su tarjeta. Verás
   el código y, debajo, un **código corto de 6 caracteres** (por si el móvil del
   cliente no muestra el QR).
3. **Escanear** — con una cuenta de personal (Paso 8), pulsa el botón flotante de
   escanear en la pantalla de Agenda y apunta al QR de la otra pantalla. Se abrirá
   la ficha de esa cita para confirmarla o atenderla.
4. **Cambiar un rol** — como admin, pestaña **Usuarios**, elige a alguien y dale
   `doctor`. Esa persona tendrá que cerrar sesión y volver a entrar.

---

## Si algo falla

| Lo que ves | Qué significa | Solución |
|---|---|---|
| `CONFIGURATION_NOT_FOUND` | No activaste el login por correo | Paso 4 |
| `API key not valid` | Sigues con el `google-services.json` falso | Paso 3 |
| `Can't determine Firebase Database URL` | Tu `google-services.json` no tiene la línea `firebase_url`, porque lo descargaste **antes** de crear la base de datos | Crea la base (Paso 5) y **vuelve a descargar** el `google-services.json` desde ⚙️ → *Configuración del proyecto* → *Tus apps* |
| `Permission denied` al leer citas | Las reglas nuevas no están publicadas | Paso 6 |
| `Index not defined` | Falta el `.indexOn` → tienes las reglas viejas | Paso 6 |
| La app entra pero no hay datos | Creaste Firestore en vez de Realtime Database | Paso 5 |
| `Default FirebaseApp is not initialized` | El `google-services.json` no está en `app/` | Paso 3 |
| Sigo viendo pantallas de cliente siendo admin | El rol se lee al iniciar sesión | Cierra sesión y vuelve a entrar |
| **La app se cierra de golpe al entrar como doctor o empleado** (con admin y cliente no) | Tienes las reglas viejas, donde `/roles` solo lo podía leer un admin. El personal necesita leerlo para saber quién es doctor | Vuelve a pegar `firebase-database-rules.json` (Paso 6) |
| **`Permission denied` al guardar un expediente desde la clínica** (se ve bien la lista, pero al pulsar *Finalizar* no se guarda) | Tienes las reglas viejas, sin el permiso de `mascotas` para el personal. Leer `/usuarios` ya estaba permitido, **escribir** en la mascota de otro cliente es nuevo | Vuelve a pegar `firebase-database-rules.json` (Paso 6) |
| En *Expedientes* → botón **+** dice que no hay ningún cliente | No hay usuarios con rol `cliente`, o las reglas viejas no dejan leer `/roles` | Registra un cliente en la app, y republica las reglas (Paso 6) |
| `Falta configurar Cloudinary` al subir una foto | Los dos valores de `local.properties` siguen en `REEMPLAZAME` | Paso 7.4 |
| Cloudinary responde `Upload preset must be whitelisted for unsigned uploads` | El preset se quedó en modo `Signed` | Paso 7.3, punto 5 |
| Cloudinary responde `Invalid cloud_name` | El `cloud_name` está mal escrito, o pusiste tu correo en su lugar | Paso 7.2 |
| La foto sube pero no se ve | Cambiaste `local.properties` y no hiciste Sync | **File → Sync Project with Gradle Files** y vuelve a ejecutar |
| El escáner de QR no abre | Falta el módulo de Google Play Services; se descarga solo la primera vez | Comprueba que el móvil tiene internet y Play Store, y reintenta |

Para ver el error real: en Android Studio, pestaña **Logcat** abajo, y filtra
escribiendo `Firebase` en la barra de búsqueda.

---

## Resumen de qué ve cada rol

| | Cliente | Empleado | Doctor | Admin |
|---|---|---|---|---|
| Sus mascotas y citas | ✅ | — | — | — |
| Ver QR de su cita | ✅ | — | — | — |
| Agenda de la clínica | — | ✅ | ✅ | ✅ |
| Escanear QR / confirmar citas | — | ✅ | ✅ | ✅ |
| Asignar doctor a una cita | — | ✅ | ✅ | ✅ |
| Ver todos los pacientes | — | ✅ | ✅ | ✅ |
| Escribir en el historial médico | — | — | ✅ | ✅ |
| Cambiar roles de usuarios | — | — | — | ✅ |

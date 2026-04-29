# Arquitectura y Backend: Firebase (Serverless)

Este documento describe la arquitectura de comunicación de datos de la aplicación **Rosa Betania Pedidos**. 

A diferencia de una arquitectura tradicional de Cliente-Servidor donde se tiene un backend propio (por ejemplo, con Node.js, Spring Boot o PHP) que provee llamados "Endpoints" HTTP (como `REST APIs`), este proyecto utiliza una arquitectura de tipo **Serverless** apoyada en **Firebase**.

## Endpoints vs SDK de Firebase

Dado que usamos **Firebase Firestore** como nuestra capa de persistencia (Base de Datos NoSQL), y **Firebase Auth** para autenticación, **no existen endpoints tradicionales** (rutas de red web usando GET, POST, PUT, DELETE). 

En su lugar, el cliente interactúa de modo directo con los servicios de la nube usando el **SDK de Firebase para Android**. Las equivalencias de las operaciones de red tradicionales se mapean a referencias directas de la base de datos de esta forma:

### 1. Crear un Pedido (Equivalente al método POST)
Para guardar información nueva en la base de datos apuntamos a la colección `pedidos` y usamos el método `.add()`. 

**Implementación Actual (`ClientActivity.kt`):**
```kotlin
val db = FirebaseFirestore.getInstance()
// La llamada a add() es nuestro "endpoint" que registra los datos
db.collection("pedidos").add(pedido)
    .addOnSuccessListener {
        // Pedido creado con éxito
    }
```

### 2. Leer o Listar Pedidos (Equivalente al método GET)
Para acceder a la información, hacemos "queries" o consultas a la misma colección. Además, a diferencia del clásico esquema de request/response, podemos suscribirnos a cambios en tiempo real usando `.addSnapshotListener()`, lo que funciona como un sistema de *WebSockets*.

**Implementación Actual (`AdminActivity.kt`):**
```kotlin
// Nuestro endpoint de lectura con suscripción a actualizaciones en tiempo real
db.collection("pedidos")
    .orderBy("fecha_creacion", Query.Direction.DESCENDING)
    .addSnapshotListener { snapshot, error -> 
        // Se ejecuta cada vez que ocurre un cambio en la BD automáticamente
    }
```

### 3. Actualizar un Pedido (Equivalente a PUT / PATCH)
Al cotizar un pedido, o cambiar el estado del mismo, sólo se deben hacer "mutaciones" a documentos individuales indicándole al sistema el identificador único `ID` de ese documento, seguido del método `.update()`.

**Implementación Actual (`AdminActivity.kt`):**
```kotlin
// Apuntamos al ID específico y mandamos las actualizaciones (PATCH)
db.collection("pedidos").document(pedido.id)
    .update(
        mapOf(
            "precio_cotizado" to price,
            "estado" to "Cotizado"
        )
    )
```

### 4. Borrar un Registro (Equivalente a DELETE)
De requerirse, para remover un pedido obsoleto el sistema utilizaría el método `.delete()` directamente en la referencia del documento.

```kotlin
// Endpoint de eliminación
db.collection("pedidos").document(pedido.id).delete()
```

## Estructura de Datos (Colecciones Críticas)

- **Colección Principal:** `pedidos`
- **Flujo de estados:** Pendiente → Cotizado → (Aprobado/Finalizado ... a definir en futuras versiones)

## Seguridad
Dado que las llamas provienen directamente del entorno en el que se ejecuta la aplicación móvil del usuario final, **la seguridad no recae en un controlador o script backend**, sino de las **Reglas de Seguridad de Firestore**. 

Estas reglas se configuran en el *Panel de Control de Firebase* (Firebase Console) para indicar qué usuarios autenticados pueden ejecutar lecturas, actualizaciones o escrituras dentro del proyecto.

# CrewShot

## Proyecto en equipo

CrewShot fue desarrollado de forma colaborativa junto a **Ainhoa Santano** y **Miguel Muñoz** como parte de una asignatura universitaria.

El proyecto nos permitió trabajar de forma conjunta en el diseño, desarrollo e integración de las distintas funcionalidades de la aplicación.

**CrewShot** es una aplicación Android de retos fotográficos desarrollada en equipo como proyecto académico.

La aplicación permite crear una experiencia social y gamificada en la que los usuarios participan en grupos, realizan retos fotográficos, comparten sus fotos y compiten mediante un sistema de votos, experiencia y rankings.

## ¿Cómo funciona?

Los usuarios pueden registrarse en la aplicación y participar en grupos en los que se proponen retos fotográficos.

A partir de estos retos pueden:

- Realizar y publicar fotografías directamente desde la aplicación.
- Compartirlas en el feed de su grupo.
- Votar las publicaciones de otros usuarios.
- Obtener puntos de experiencia (XP) a partir de los votos recibidos.
- Consultar el ranking de participantes del grupo.
- Visualizar su progreso mediante un sistema gamificado con mascota.
- Recibir notificaciones cuando comienza un nuevo reto.

## Tecnologías utilizadas

- **Kotlin**
- **Android Studio**
- **Jetpack Compose** para el desarrollo de la interfaz.
- **MVVM / ViewModels** para separar la interfaz de usuario y la lógica de la aplicación.
- **Firebase** para los servicios backend de la aplicación.
- **Firebase Authentication** para el registro e inicio de sesión.
- **Firebase Cloud Messaging (FCM)** para el envío de notificaciones.
- **Gradle** para la gestión y construcción del proyecto.

## Arquitectura

El proyecto sigue una estructura basada en **MVVM**, utilizando ViewModels y repositorios para separar responsabilidades y facilitar la gestión del estado y de los datos.

Entre los principales componentes de la aplicación se encuentran:

- `FeedScreen`: visualización de las publicaciones del grupo.
- `CameraScreen`: captura y publicación de fotografías.
- `PostViewModel`: gestión de las publicaciones.
- `LeaderboardViewModel`: gestión del ranking y las puntuaciones.
- `GroupViewModel`: gestión de los grupos y su información.

## Autenticación

CrewShot incorpora un sistema de autenticación mediante Firebase que permite a los usuarios crear una cuenta e iniciar sesión mediante email y contraseña.

Cada usuario dispone además de un nombre de usuario asociado a su perfil dentro de la aplicación.

## Gamificación

Uno de los elementos principales del proyecto es la incorporación de mecánicas de gamificación.

Las interacciones con las fotografías permiten obtener **XP**, que representa la progresión del usuario y se utiliza también para establecer los rankings dentro de cada grupo.

## Retos y notificaciones

La aplicación utiliza **Firebase Cloud Messaging (FCM)** para comunicar el comienzo de nuevos retos a los participantes, permitiendo renovar periódicamente la dinámica y el contenido de los grupos.

## Ejecución

1. Clonar el repositorio:

```bash
git clone URL_DEL_REPOSITORIO

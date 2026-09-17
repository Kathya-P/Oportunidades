# Oportunidades

Sistema web de gestión académica y control de asistencia desarrollado para el Programa Oportunidades de la Fundación Gloria Kriete.

La plataforma centraliza la administración de estudiantes, docentes, grupos, períodos escolares y registros de asistencia. También transforma los datos de asistencia en indicadores de seguimiento, como tardanzas, ausencias y niveles de riesgo de ausentismo.

## ¿Qué problema resuelve?

El proyecto reemplaza procesos manuales de control de asistencia por una plataforma centralizada. Permite registrar entradas y salidas mediante códigos de barras, consultar el comportamiento de los grupos y generar información útil para detectar estudiantes que requieren seguimiento.

## Funcionalidades principales

- Autenticación de usuarios con Spring Security.
- Control de acceso basado en roles.
- Administración de usuarios, estudiantes, docentes y grupos.
- Registro de asistencia mediante lector de códigos de barras.
- Registro de entradas, salidas, tardanzas y ausencias.
- Gestión de períodos escolares y calendario académico.
- Bloqueo de días en los que no corresponde registrar asistencia.
- Dashboard con estadísticas generales y estadísticas por grupo.
- Detección de riesgo de ausentismo.
- Clasificación de estudiantes en niveles de precaución y riesgo crítico.
- Reportes descargables en PDF, Excel y JasperReports.
- Envío de correos para recuperación o restablecimiento de contraseñas.
- Interfaz web responsive con vistas diferenciadas por rol.

## Roles del sistema

### Administrador

Gestiona usuarios, estudiantes, grupos, períodos escolares y la configuración general de la plataforma.

### Docente

Consulta sus grupos, registra asistencias y revisa el historial de los estudiantes asignados.

### Supervisor

Consulta estadísticas globales, reportes, estudiantes y métricas de riesgo de ausentismo.

## Flujo principal

```text
Usuario inicia sesión
        |
        v
Accede según su rol
        |
        v
Administra grupos o registra asistencia
        |
        v
El sistema procesa ausencias y tardanzas
        |
        v
Supervisión mediante dashboards y reportes
```

## Tecnologías utilizadas

- Java 21
- Spring Boot
- Spring MVC
- Thymeleaf
- Spring Security
- Spring Data JPA y Hibernate
- PostgreSQL
- Supabase
- JasperReports
- Apache POI
- iTextPDF
- ZXing para códigos de barras
- Docker
- Maven

## Arquitectura

El proyecto utiliza una arquitectura MVC organizada por responsabilidades:

- `controllers`: reciben las solicitudes y coordinan las vistas.
- `services`: contienen la lógica de negocio.
- `repositories`: gestionan el acceso a datos mediante Spring Data JPA.
- `models`: representan las entidades y relaciones del dominio.
- `security`: contiene la autenticación y autorización.
- `templates`: vistas HTML renderizadas con Thymeleaf.
- `reports`: plantillas utilizadas para generar reportes.

La aplicación se conecta a PostgreSQL mediante variables de entorno, por lo que puede desplegarse sin guardar credenciales dentro del código fuente.

## Base de datos y despliegue

La base de datos utiliza PostgreSQL administrado por Supabase. El backend Spring Boot se empaqueta en Docker y puede desplegarse en servicios como Render.

El proyecto está preparado para:

- Escuchar el puerto asignado por la plataforma de despliegue mediante `PORT`.
- Utilizar Supabase Connection Pooling para conexiones externas.
- Crear o actualizar las tablas mediante Hibernate.
- Ejecutarse como un contenedor Docker basado en Java 21.

## Variables de entorno

```text
SUPABASE_DB_HOST
SUPABASE_DB_PORT
SUPABASE_DB_NAME
SUPABASE_DB_USER
SUPABASE_DB_PASSWORD
MAIL_USERNAME
MAIL_PASSWORD
PORT
```

Las credenciales no se almacenan en el repositorio. Para el envío de correos se utiliza una contraseña de aplicación de Gmail.

## Datos de demostración

El archivo `supabase-seed.sql` contiene datos de prueba para mostrar el funcionamiento de la plataforma, incluyendo:

- Usuarios con diferentes roles.
- Grupos con distintas modalidades.
- Estudiantes e inscripciones.
- Registros de asistencia.
- Entradas, salidas, tardanzas y ausencias.
- Un período escolar activo.

## Objetivo del proyecto

Crear una herramienta web que facilite el control de asistencia y convierta los registros diarios en información útil para la gestión académica y el seguimiento oportuno de estudiantes.

# Resumen para exponer — Spring Security (control de acceso por roles)

**Proyecto:** Sistema de Asistencia FGK — Oportunidades  
**Tema evaluado:** Seguridad con roles (proteger rutas y controlar acceso)

---

## 1) ¿Qué problema resuelve?

En un sistema real no cualquiera debería ver o modificar información.
Por eso implementé seguridad para:

- Pedir **inicio de sesión** (usuario y contraseña).
- Definir **permisos** según el tipo de usuario (rol).
- Bloquear el acceso a páginas que no corresponden.

---

## 2) ¿Cómo funciona el inicio de sesión?

1. El usuario entra a la pantalla de **Login**.
2. Escribe su **usuario** y **contraseña**.
3. El sistema busca ese usuario en la base de datos.
4. Si la contraseña es correcta, el usuario entra.
5. Si es incorrecta, vuelve al login con un aviso.

---

## 3) ¿Qué son los roles?

Un **rol** es el “tipo de usuario” y define qué puede hacer.
En el proyecto se manejan roles como:

- **ADMIN**: administra usuarios y secciones.
- **DOCENTE**: consulta asistencias.
- **SUPERVISOR**: ve reportes / estadísticas.

---

## 4) ¿Cómo se protegen las rutas (páginas) según el rol?

El sistema tiene reglas por URL:

- **Público:**
  - `/login`
- **Solo ADMIN:**
  - todo lo que comienza con `/admin/`
- **Solo DOCENTE:**
  - todo lo que comienza con `/docente/`
- **Solo SUPERVISOR:**
  - todo lo que comienza con `/supervisor/`

Si alguien intenta entrar a una zona restringida:

- Si **no** inició sesión → el sistema lo manda al login.
- Si **sí** inició sesión pero **no** tiene el rol → no debería permitir el acceso.

---

## 5) ¿Qué pasa después de iniciar sesión?

Cuando el usuario inicia sesión, el sistema revisa su rol y lo manda a su área:

- ADMIN → área de administración.
- DOCENTE → área de docente.
- SUPERVISOR → área de supervisor.

---

## 6) ¿Cómo se manejan las contraseñas?

Las contraseñas se guardan de forma segura (no en texto plano).
Eso evita que alguien pueda ver la contraseña real si accede a la base de datos.

---

## 7) ¿Cómo se cierra sesión?

El usuario puede cerrar sesión con **logout**.
Al cerrar sesión, el sistema termina la sesión y vuelve al login.

---

## Conclusión

Con Spring Security implementé:

- Inicio de sesión.
- Control de acceso por roles.
- Protección de rutas.

Esto cumple el requisito de **controlar quién entra y a qué páginas puede acceder** según su rol.

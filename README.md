# Supermercado — Multi-Process Java Console System

> 🚧 **Work in Progress** — All four applications are implemented and tested with automated console tests. The final manual test in a real Windows console is pending.

A console-based supermarket management system made of **four independent Java applications**. A main application launches the others as real operating-system processes, and they share data stored in two XML files.

---

## Table of Contents

- [Project Overview](#project-overview)
- [Project Objectives](#project-objectives)
- [Technologies](#technologies)
- [Main Features](#main-features)
- [Project Architecture](#project-architecture)
- [XML Data Storage](#xml-data-storage)
- [Installation and Execution](#installation-and-execution)
- [Screenshots](#screenshots)
- [Project Status](#project-status)
- [Author](#author)

---

## Project Overview

This is an academic intermodular project that combines two subjects:

- **Services and Processes Programming** — creating, running and controlling processes.
- **Data Access** — reading, writing and modifying XML files with the DOM API.

The system is designed for a supermarket chain that needs to register customers, manage their purchases and generate invoices. It is split into four independent console applications:

| Application      | Role |
|------------------|------|
| **Supermercado** | Main menu. Launches the other three applications as separate processes and waits for them to finish. |
| **Clientes**     | Registers, deletes and lists customers. |
| **Compras**      | Manages each customer's pending purchase: add, view, modify, delete and save products. |
| **Facturacion**  | Shows pending purchases, calculates totals and discounts, and confirms invoices. |

> Source code identifiers and console messages are written in Spanish, the language of the course.

---

## Project Objectives

- Create and control real operating-system processes from Java with `ProcessBuilder`.
- Share the console between the parent and child processes with `inheritIO()`.
- Wait for each child process with `waitFor()` and check its exit code.
- Create, read, update and delete XML data with the Java DOM API.
- Keep data persistent between executions.
- Validate user input so the applications do not stop unexpectedly.
- Perform accurate monetary calculations with `BigDecimal`.

---

## Technologies

| Technology | Usage |
|------------|-------|
| Java (JDK 26, targeting Java 21) | Programming language |
| Apache NetBeans 31 | IDE — four *Java with Ant* projects |
| Apache Ant | Build tool bundled with NetBeans |
| Java DOM API | `DocumentBuilderFactory`, `DocumentBuilder`, `Document`, `Element`, `NodeList`, `Transformer` |
| `ProcessBuilder` | Process creation and control |
| Git & GitHub | Version control |

No external libraries and no Maven: only the standard Java library.

---

## Main Features

### Supermercado (main application)
- [x] Main menu that stays active until the user chooses to exit
- [x] Launch `Clientes.jar`, `Compras.jar` and `Facturacion.jar` with `ProcessBuilder`
- [x] Interactive console for child processes (`inheritIO()`)
- [x] Wait for each process (`waitFor()`) and check its exit code
- [x] Error handling when a JAR cannot be found or started
- [x] Java executable resolved from `java.home` (no dependency on the system `PATH`)

### Clientes
- [x] Register customers with an automatically generated unique ID (deleted IDs are never reused)
- [x] Delete a customer (with confirmation) together with their pending purchase
- [x] List registered customers

### Compras
- [x] Select an existing customer and load their pending products
- [x] Add, view, modify and delete products
- [x] Price validation (greater than zero, maximum two decimals, comma or dot) and positive integer quantities
- [x] Save the purchase without duplicates and without affecting other customers' data
- [x] Warning about unsaved changes (save / discard / cancel)

### Facturacion
- [x] Select a customer and view their pending purchase
- [x] Calculate line amounts, subtotal, 10 % discount (subtotal ≥ 50 €) and final total with `BigDecimal`
- [x] Confirm the invoice and remove only that customer's pending purchase from `compras.xml`

### Example: Invoice

Output of the Facturacion application, identical to the example in the assignment:

```
========== FACTURA ==========

Cliente: Juan
ID cliente: 1

Producto       Cant.   Precio   Importe

Arroz            2     3,50 €     7,00 €
Aceite           3    15,00 €    45,00 €

---------------------------------------
Subtotal:                      52,00 €
Descuento (10 %):               5,20 €
TOTAL A PAGAR:                 46,80 €
---------------------------------------
```

---

## Project Architecture

```mermaid
flowchart TD
    U([User / Console]) --> S[Supermercado.jar]

    S -->|"ProcessBuilder + inheritIO + waitFor"| C[Clientes.jar]
    S -->|"ProcessBuilder + inheritIO + waitFor"| P[Compras.jar]
    S -->|"ProcessBuilder + inheritIO + waitFor"| F[Facturacion.jar]

    subgraph DATOS["datos/ — shared XML files"]
        XC[(clientes.xml)]
        XP[(compras.xml)]
    end

    C <-->|"read / write"| XC
    C <-->|"read / delete purchase"| XP
    P -->|"read"| XC
    P <-->|"read / write"| XP
    F -->|"read"| XC
    F <-->|"read / delete purchase"| XP
```

All applications use the same common `datos` folder. Supermercado controls the processes, while Clientes, Compras and Facturacion read and modify the two shared XML files.

**Process flow**

1. The user starts `Supermercado.jar`.
2. When an option is selected, Supermercado starts the corresponding JAR as a new process.
3. The child process uses the same console, so the user interacts with it directly.
4. Supermercado waits until the child process ends, checks its exit code and shows the main menu again.

**Folder structure**

```
Supermercado-Java/
├── Supermercado/      # NetBeans project + Supermercado.jar
├── Clientes/          # NetBeans project + Clientes.jar
├── Compras/           # NetBeans project + Compras.jar
├── Facturacion/       # NetBeans project + Facturacion.jar
├── datos/             # Shared XML files
│   ├── clientes.xml
│   └── compras.xml
├── README.md
└── README.txt         # Execution instructions for the course
```

---

## XML Data Storage

All applications share two XML files located in the common `datos` folder. They are always accessed through the DOM API, never by string concatenation.

**`clientes.xml`** (example)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<clientes>
    <cliente id="1">
        <nombre>Juan</nombre>
    </cliente>
</clientes>
```

**`compras.xml`** (example)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<compras>
    <compra clienteId="1">
        <producto id="1">
            <nombre>Leche</nombre>
            <precio>1.20</precio>
            <cantidad>2</cantidad>
        </producto>
    </compra>
</compras>
```

**Storage rules**

- UTF-8 encoding.
- If a file does not exist, it is created with its root element.
- Each customer has at most one pending purchase.
- Prices are stored with a dot as decimal separator.
- Product IDs are unique within each customer's purchase.
- All applications find the same `datos` folder regardless of the directory they are run from.
- Files are saved safely: first to a temporary `.tmp` file, which then replaces the original, so an XML file is never left half-written.
- A malformed XML file is reported to the user and never overwritten.
- `clientes.xml` may store an optional `ultimoId` attribute on its root element (`<clientes ultimoId="3">`), so deleted customer IDs are never reused.

---

## Installation and Execution

**Requirements**

- Java Runtime 21 or later.

**Clone the repository**

```bash
git clone https://github.com/nataliekhuri/Supermercado-Java.git
cd Supermercado-Java
```

**Run**

```bash
java -jar Supermercado/Supermercado.jar
```

If `java` is not available in the system `PATH`, use the full path to the Java executable.
Running from a terminal (CMD) is recommended: the NetBeans Output window does not always forward keyboard input to the child processes.

**Build from source**

Open each project in Apache NetBeans and run **Clean and Build**. The JAR file is copied automatically into its application folder.

---

## Screenshots

*Screenshots will be added soon.*

---

## Project Status

| Phase | Description | Status |
|-------|-------------|--------|
| 0 | Repository setup (`.gitignore`, `datos`, README) | ✅ Completed |
| 1 | NetBeans projects and process control (`ProcessBuilder`) | ✅ Completed |
| 2 | Clientes application | ✅ Completed |
| 3–4 | Compras application | ✅ Completed |
| 5 | Facturacion application | ✅ Completed |
| 6 | Full testing | 🔄 In progress — final manual test in CMD pending |
| 7 | Final delivery and documentation | 🔄 In progress |

---

## Author

**Natalie Khuri** — GitHub: [nataliekhuri](https://github.com/nataliekhuri)
